package pl.wizytowka;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.support.TestcontainersConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pełna ścieżka przez wszystkie moduły i prawdziwe adaptery (HTTP → bezpieczeństwo → aplikacja → PostgreSQL),
 * łącznie z prawdziwym generowaniem PDF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class})
@TestPropertySource(properties = {
        "showroom.security.jwt.secret=c2hvd3Jvb20tZTJlLXNlY3JldC13aGljaC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTY=",
        "showroom.security.bootstrap-admin.email=owner@brand.test",
        "showroom.security.bootstrap-admin.password=correct-horse-battery",
        "showroom.storage.local-root=target/e2e-storage",
        "showroom.rate-limit.enabled=false"
})
class ShowroomEndToEndTest {

    static final String BUYER_LOGIN = """
            {"email":"buyer@boutique.cz","password":"correct horse battery staple"}""";

    @Autowired
    MockMvc mvc;

    /** Prawdziwy adapter SMTP podmieniony — test przechwytuje tokeny z e-maili. */
    @MockitoBean
    BuyerMailer mailer;

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

        // --- publiczny katalog (bez tokenu), język z nagłówka — bez cen i EAN (D23)
        MvcResult listing = mvc.perform(get("/api/v1/catalog/products").header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Linen shirt"))
                .andExpect(jsonPath("$.items[0].price").doesNotExist())
                .andReturn();
        String imageUrl = JsonPath.read(listing.getResponse().getContentAsString(), "$.items[0].cover.url");
        mvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("immutable")));
        mvc.perform(get("/api/v1/catalog/products/koszula-lniana").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Koszula lniana"))
                .andExpect(jsonPath("$.colors[0].variants[0].sku").value("LS1-NAV-M"))
                .andExpect(jsonPath("$.colors[0].variants[0].ean").doesNotExist());
        mvc.perform(get("/api/v1/buyer/catalog/pricing").param("product", productId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/buyer/line-sheets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        // --- kupiec: rejestracja → potwierdzenie adresu → akceptacja marki → logowanie
        mvc.perform(buyerAuth("/register").param("lang", "en").content("""
                        {"email":"Buyer@Boutique.cz","password":"correct horse battery staple",
                         "companyName":"Boutique s.r.o.","countryCode":"CZ","taxId":"CZ12345678",
                         "contactName":"Jana Nováková","phone":"+420 777 100 200",
                         "termsAccepted":true,"privacyAccepted":true}"""))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> verificationToken = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendEmailVerification(eq("buyer@boutique.cz"), eq(CatalogLanguage.EN), verificationToken.capture());
        mvc.perform(buyerAuth("/verify-email").content("""
                        {"token":"%s"}""".formatted(verificationToken.getValue())))
                .andExpect(status().isNoContent());
        verify(mailer).notifyStaffAboutPendingBuyer(List.of("owner@brand.test"));

        mvc.perform(buyerAuth("/login").content(BUYER_LOGIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("identity.buyer.not-active"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        mvc.perform(get("/api/v1/admin/buyers/pending-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        String buyers = mvc.perform(get("/api/v1/admin/buyers").param("status", "PENDING_APPROVAL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].taxId").value("12345678"))
                .andReturn().getResponse().getContentAsString();
        String buyerId = JsonPath.read(buyers, "$.items[0].id");
        long buyerVersion = ((Number) JsonPath.read(mvc.perform(get("/api/v1/admin/buyers/" + buyerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString(), "$.version")).longValue();
        mvc.perform(post("/api/v1/admin/buyers/" + buyerId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d}""".formatted(buyerVersion)))
                .andExpect(status().isNoContent());
        verify(mailer).sendAccountApproved("buyer@boutique.cz", CatalogLanguage.EN);

        MvcResult buyerLogin = mvc.perform(buyerAuth("/login").content(BUYER_LOGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("showroom_buyer_refresh=")))
                .andExpect(jsonPath("$.buyer.status").value("ACTIVE"))
                .andReturn();
        String buyerToken = JsonPath.read(buyerLogin.getResponse().getContentAsString(), "$.accessToken");

        // --- kupiec: ceny i EAN, zestawienie PDF z danymi konta
        mvc.perform(get("/api/v1/buyer/catalog/pricing").param("product", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price.wholesale").value(149.00))
                .andExpect(jsonPath("$[0].variants[0].ean").value("5901234123457"));
        mvc.perform(get("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
        // token panelu nie daje dostępu do API kupca
        mvc.perform(get("/api/v1/buyer/catalog/pricing").param("product", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        MvcResult created = mvc.perform(post("/api/v1/buyer/line-sheets").param("lang", "en")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"variantId":"%s","quantity":12}]}""".formatted(variantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNumber").value(startsWith("LS-")))
                .andReturn();
        String pdfUrl = JsonPath.read(created.getResponse().getContentAsString(), "$.pdfUrl");
        mvc.perform(get(pdfUrl)).andExpect(status().isUnauthorized());
        byte[] pdf = mvc.perform(get(pdfUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        mvc.perform(get("/api/v1/buyer/line-sheets").header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        String lineSheets = mvc.perform(get("/api/v1/admin/line-sheets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("buyer@boutique.cz"))
                .andExpect(jsonPath("$.items[0].companyName").value("Boutique s.r.o."))
                .andExpect(jsonPath("$.items[0].buyerAccountId").value(buyerId))
                .andExpect(jsonPath("$.items[0].emailStatus").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/admin/line-sheets/" + JsonPath.read(lineSheets, "$.items[0].id") + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
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

    private static MockHttpServletRequestBuilder buyerAuth(String path) {
        return post("/api/v1/buyer/auth" + path)
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON);
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
