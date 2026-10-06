package pl.wizytowka;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.wizytowka.support.LineSheetOutputFakesConfiguration;
import pl.wizytowka.support.LineSheetOutputFakesConfiguration.RecordingLineSheetMailer;
import pl.wizytowka.support.TestcontainersConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pełna ścieżka przez wszystkie moduły i prawdziwe adaptery (HTTP → bezpieczeństwo → aplikacja → PostgreSQL),
 * z podmienionym wyłącznie generowaniem PDF i wysyłką e-mail (adaptery Etapu 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, LineSheetOutputFakesConfiguration.class})
@TestPropertySource(properties = {
        "showroom.security.jwt.secret=c2hvd3Jvb20tZTJlLXNlY3JldC13aGljaC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTY=",
        "showroom.security.bootstrap-admin.email=owner@brand.test",
        "showroom.security.bootstrap-admin.password=correct-horse-battery",
        "showroom.storage.local-root=target/e2e-storage"
})
class ShowroomEndToEndTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RecordingLineSheetMailer mailer;

    @Test
    void adminPublishesProductAndBuyerReceivesLineSheet() throws Exception {
        String token = login();

        // --- panel: słowniki, produkt, wariant, zdjęcie, publikacja
        String categoryId = id(admin(post("/api/v1/admin/categories"), token, """
                {"slug":"koszule","sortOrder":0,"visible":true,
                 "translations":{"pl":{"name":"Koszule"},"en":{"name":"Shirts"}}}"""));
        String colorId = id(admin(post("/api/v1/admin/colors"), token, """
                {"code":"NAVY","hex":"#1c2340","sortOrder":0,"names":{"pl":"Granatowy","en":"Navy"}}"""));
        String sizeId = id(admin(post("/api/v1/admin/sizes"), token, """
                {"code":"M","sortOrder":30}"""));
        String productId = id(admin(post("/api/v1/admin/products"), token, """
                {"categoryId":"%s","slug":"koszula-lniana","styleCode":"LS-001",
                 "wholesalePrice":149.00,"retailPrice":349.99,"featured":true,"sortOrder":0,
                 "translations":{"pl":{"name":"Koszula lniana","materialComposition":"100%% len"},
                                 "en":{"name":"Linen shirt"}}}""".formatted(categoryId)));
        String variantId = id(admin(post("/api/v1/admin/products/" + productId + "/variants"), token, """
                {"colorId":"%s","sizeId":"%s","sku":"ls1-nav-m","ean":"5901234123457"}""".formatted(colorId, sizeId)));
        mvc.perform(multipart("/api/v1/admin/products/" + productId + "/images")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png()))
                        .param("colorId", colorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());
        long version = ((Number) JsonPath.read(mvc.perform(get("/api/v1/admin/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.version")).longValue();
        mvc.perform(post("/api/v1/admin/products/" + productId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transition":"PUBLISH","version":%d}""".formatted(version)))
                .andExpect(status().isNoContent());
        id(admin(post("/api/v1/admin/sales-representatives"), token, """
                {"fullName":"Anna Nowak","email":"anna@brand.test","phone":"+48 600 000 000","active":true,"sortOrder":0}"""));

        // --- publiczny katalog (bez tokenu), język z nagłówka
        MvcResult listing = mvc.perform(get("/api/v1/catalog/products").header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Linen shirt"))
                .andExpect(jsonPath("$.items[0].price.wholesale").value(149.00))
                .andReturn();
        String imageUrl = JsonPath.read(listing.getResponse().getContentAsString(), "$.items[0].cover.url");
        mvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("immutable")));
        mvc.perform(get("/api/v1/catalog/products/koszula-lniana").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Koszula lniana"))
                .andExpect(jsonPath("$.colors[0].variants[0].sku").value("LS1-NAV-M"));

        // --- klient B2B: zestawienie z wysyłką e-mail i pobranie PDF
        MvcResult created = mvc.perform(post("/api/v1/line-sheets").param("lang", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"variantId":"%s","quantity":12}],
                                 "companyName":"Boutique s.r.o.","email":"Buyer@Boutique.cz",
                                 "privacyAccepted":true,"sendEmail":true}""".formatted(variantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNumber").value(startsWith("LS-")))
                .andReturn();
        String pdfUrl = JsonPath.read(created.getResponse().getContentAsString(), "$.pdfUrl");
        byte[] pdf = mvc.perform(get(pdfUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).isEqualTo(LineSheetOutputFakesConfiguration.FAKE_PDF);
        // wysyłka następuje synchronicznie po commicie (asynchroniczność i ponowienia — Etap 3)
        assertThat(mailer.sent()).singleElement()
                .satisfies(document -> assertThat(document.recipient().email()).isEqualTo("buyer@boutique.cz"));

        // --- analityka: zdarzenie z przeglądarki + dashboard
        mvc.perform(post("/api/v1/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"events":[{"type":"PRODUCT_VIEW","productId":"%s","path":"/en/products/koszula-lniana"}]}"""
                                .formatted(productId)))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/admin/analytics/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineSheetsCreated").value(1))
                .andExpect(jsonPath("$.topViewedProducts[0].productId").value(productId))
                .andExpect(jsonPath("$.topLineSheetProducts[0].productId").value(productId));

        // --- granice dostępu
        mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/media/line-sheets/x.pdf")).andExpect(status().isNotFound());
    }

    private String login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/admin/auth/login")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@brand.test","password":"correct-horse-battery"}"""))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcResult admin(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private static String id(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(40, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
