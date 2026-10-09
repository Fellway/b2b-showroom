package pl.wizytowka.catalog.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageProductImagesUseCase;
import pl.wizytowka.catalog.application.port.in.ManageProductsUseCase;
import pl.wizytowka.catalog.application.port.in.StatusTransition;
import pl.wizytowka.catalog.application.view.AdminProductFilter;
import pl.wizytowka.catalog.application.view.AdminProductListItem;
import pl.wizytowka.catalog.application.view.AdminProductView;
import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.ImageView;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.Gtin;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.catalog.domain.ProductStatus;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.catalog.domain.Sku;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.Pricing;
import pl.wizytowka.shared.domain.ResourceConflictException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class AdminProductControllerTest {

    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID CATEGORY = UUID.randomUUID();
    static final UUID COLOR = UUID.randomUUID();
    static final UUID SIZE = UUID.randomUUID();
    static final PriceView PRICE = new PriceView(new BigDecimal("149.00"), new BigDecimal("349.99"), "PLN", new BigDecimal("2.35"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageProductsUseCase products;

    @MockitoBean
    ManageProductImagesUseCase images;

    @MockitoBean
    AdminCatalogQueryUseCase queries;

    static String productJson(String translations, String extra) {
        return """
                {"categoryId":"%s","slug":"koszula-lniana","styleCode":"ls-001",
                 "wholesalePrice":149.00,"retailPrice":349.99,"featured":true,"sortOrder":3,
                 "translations":%s%s}
                """.formatted(CATEGORY, translations, extra);
    }

    static final String TRANSLATIONS = """
            {"pl":{"name":"Koszula lniana","materialComposition":"100% len"},"en":{"name":"Linen shirt"}}""";

    @Test
    void createsProductMappingTranslationsAndDefaultCurrency() throws Exception {
        UUID created = UUID.randomUUID();
        when(products.create(any())).thenReturn(ProductId.of(created));

        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(TRANSLATIONS, "")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/admin/products/" + created))
                .andExpect(jsonPath("$.id").value(created.toString()));

        ArgumentCaptor<ProductDetails> details = ArgumentCaptor.forClass(ProductDetails.class);
        verify(products).create(details.capture());
        ProductDetails value = details.getValue();
        assertThat(value.categoryId()).isEqualTo(CategoryId.of(CATEGORY));
        assertThat(value.styleCode()).isEqualTo("LS-001");
        assertThat(value.pricing()).isEqualTo(Pricing.pln("149.00", "349.99"));
        assertThat(value.featured()).isTrue();
        assertThat(value.sortOrder()).isEqualTo(3);
        assertThat(value.translations().resolve(CatalogLanguage.PL).materialComposition()).isEqualTo("100% len");
        assertThat(value.translations().resolve(CatalogLanguage.EN).name()).isEqualTo("Linen shirt");
        assertThat(value.translations().exact(CatalogLanguage.CS)).isEmpty();
    }

    @Test
    void rejectsTranslationsWithoutDefaultLanguageOrUnknownLanguage() throws Exception {
        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("{\"en\":{\"name\":\"Linen shirt\"}}", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("i18n.default-language-required"));

        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("{\"pl\":{\"name\":\"Koszula\"},\"de\":{\"name\":\"Hemd\"}}", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("i18n.unsupported-language"));
        verifyNoInteractions(products);
    }

    @Test
    void rejectsInvalidRequestWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"","styleCode":"X","wholesalePrice":10,"retailPrice":20,
                                 "currency":"pln","translations":{"pl":{"name":"A"}}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid"))
                .andExpect(jsonPath("$.errors[?(@.field=='categoryId')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='slug')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='currency')]").exists());

        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(TRANSLATIONS, ",\"currency\":\"XYZ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("pricing.currency"));

        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(products);
    }

    @Test
    void updateRequiresVersionAndMapsConcurrentModificationToConflict() throws Exception {
        mvc.perform(put("/api/v1/admin/products/{id}", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(TRANSLATIONS, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.version-required"));

        mvc.perform(put("/api/v1/admin/products/{id}", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(TRANSLATIONS, ",\"version\":7")))
                .andExpect(status().isNoContent());
        verify(products).update(eq(ProductId.of(PRODUCT)), eq(7L), any(ProductDetails.class));

        doThrow(new ConcurrentModificationException("data.concurrent-modification", "Modified"))
                .when(products).update(any(), anyLong(), any());
        mvc.perform(put("/api/v1/admin/products/{id}", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(TRANSLATIONS, ",\"version\":6")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data.concurrent-modification"));
    }

    @Test
    void changesStatusAndMapsBusinessRuleTo422() throws Exception {
        mvc.perform(post("/api/v1/admin/products/{id}/status", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transition\":\"HIDE\",\"version\":2}"))
                .andExpect(status().isNoContent());
        verify(products).changeStatus(ProductId.of(PRODUCT), 2L, StatusTransition.HIDE);

        doThrow(new BusinessRuleViolationException("catalog.product.publish-without-variants", "No variants"))
                .when(products).changeStatus(any(), anyLong(), eq(StatusTransition.PUBLISH));
        mvc.perform(post("/api/v1/admin/products/{id}/status", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transition\":\"PUBLISH\",\"version\":2}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("catalog.product.publish-without-variants"));

        mvc.perform(post("/api/v1/admin/products/{id}/status", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transition\":\"DESTROY\",\"version\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managesColorsAndVariants() throws Exception {
        UUID variant = UUID.randomUUID();
        when(products.addVariant(any(), any(), any(), any(), any())).thenReturn(VariantId.of(variant));

        mvc.perform(post("/api/v1/admin/products/{id}/colors", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorId\":\"%s\"}".formatted(COLOR)))
                .andExpect(status().isNoContent());
        verify(products).addColor(ProductId.of(PRODUCT), ColorId.of(COLOR));

        mvc.perform(put("/api/v1/admin/products/{id}/colors/order", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorIds\":[\"%s\"]}".formatted(COLOR)))
                .andExpect(status().isNoContent());
        verify(products).reorderColors(ProductId.of(PRODUCT), List.of(ColorId.of(COLOR)));

        mvc.perform(post("/api/v1/admin/products/{id}/variants", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"colorId":"%s","sizeId":"%s","sku":"ls001-nav-m","ean":"5901234123457"}"""
                                .formatted(COLOR, SIZE)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/v1/admin/products/%s/variants/%s".formatted(PRODUCT, variant)))
                .andExpect(jsonPath("$.id").value(variant.toString()));
        verify(products).addVariant(ProductId.of(PRODUCT), ColorId.of(COLOR), SizeId.of(SIZE),
                Sku.of("LS001-NAV-M"), Gtin.of("5901234123457"));

        mvc.perform(put("/api/v1/admin/products/{id}/variants/{v}", PRODUCT, variant).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"LS001-NAV-M\",\"ean\":\"5901234123457\",\"active\":false}"))
                .andExpect(status().isNoContent());
        verify(products).updateVariant(ProductId.of(PRODUCT), VariantId.of(variant), Sku.of("LS001-NAV-M"),
                Gtin.of("5901234123457"), false);

        mvc.perform(delete("/api/v1/admin/products/{id}/variants/{v}", PRODUCT, variant))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/products/{id}/colors/{c}", PRODUCT, COLOR))
                .andExpect(status().isNoContent());
        verify(products).removeVariant(ProductId.of(PRODUCT), VariantId.of(variant));
        verify(products).removeColor(ProductId.of(PRODUCT), ColorId.of(COLOR));
    }

    @Test
    void rejectsInvalidEanAndMapsDuplicateSkuToConflict() throws Exception {
        mvc.perform(post("/api/v1/admin/products/{id}/variants", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"colorId":"%s","sizeId":"%s","sku":"A-1","ean":"5901234123458"}""".formatted(COLOR, SIZE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.ean.check-digit"));

        when(products.addVariant(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceConflictException("catalog.variant.sku-taken", "SKU already used"));
        mvc.perform(post("/api/v1/admin/products/{id}/variants", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"colorId":"%s","sizeId":"%s","sku":"A-1","ean":"5901234123457"}""".formatted(COLOR, SIZE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("catalog.variant.sku-taken"));
    }

    @Test
    void uploadsImagePassingRawBytesAndOptionalColor() throws Exception {
        UUID image = UUID.randomUUID();
        byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        when(images.upload(any(), any(), any())).thenReturn(ImageId.of(image));

        mvc.perform(multipart("/api/v1/admin/products/{id}/images", PRODUCT)
                        .file(new MockMultipartFile("file", "../../evil.jpg", "text/html", bytes))
                        .param("colorId", COLOR.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(image.toString()));
        verify(images).upload(ProductId.of(PRODUCT), ColorId.of(COLOR), bytes);

        mvc.perform(multipart("/api/v1/admin/products/{id}/images", PRODUCT)
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes)))
                .andExpect(status().isCreated());
        verify(images).upload(ProductId.of(PRODUCT), null, bytes);
    }

    @Test
    void rejectsEmptyOrMissingUpload() throws Exception {
        mvc.perform(multipart("/api/v1/admin/products/{id}/images", PRODUCT)
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.image.empty"));

        mvc.perform(multipart("/api/v1/admin/products/{id}/images", PRODUCT))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(images);
    }

    @Test
    void managesImageOrderColorAndRemoval() throws Exception {
        UUID image = UUID.randomUUID();

        mvc.perform(put("/api/v1/admin/products/{id}/images/order", PRODUCT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageIds\":[\"%s\"]}".formatted(image)))
                .andExpect(status().isNoContent());
        verify(images).reorder(ProductId.of(PRODUCT), List.of(ImageId.of(image)));

        mvc.perform(put("/api/v1/admin/products/{id}/images/{i}/color", PRODUCT, image).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorId\":null}"))
                .andExpect(status().isNoContent());
        verify(images).assignColor(ProductId.of(PRODUCT), ImageId.of(image), null);

        mvc.perform(delete("/api/v1/admin/products/{id}/images/{i}", PRODUCT, image))
                .andExpect(status().isNoContent());
        verify(images).remove(ProductId.of(PRODUCT), ImageId.of(image));
    }

    @Test
    void listsAndReadsProducts() throws Exception {
        Instant now = Instant.parse("2026-10-05T10:00:00Z");
        UUID image = UUID.randomUUID();
        UUID variant = UUID.randomUUID();
        when(queries.products(any(), any())).thenReturn(new PageResult<>(List.of(new AdminProductListItem(
                PRODUCT, "koszula-lniana", "LS-001", "Koszula lniana", "Koszule", ProductStatus.DRAFT, PRICE,
                1, 2, 2, List.of(CatalogLanguage.CS, CatalogLanguage.UK), null, now, 4)), 0, 25, 1));
        when(queries.product(ProductId.of(PRODUCT))).thenReturn(new AdminProductView(PRODUCT, CATEGORY,
                "koszula-lniana", "LS-001", ProductStatus.PUBLISHED, PRICE, false, 0, now,
                Map.of(CatalogLanguage.EN, ProductTranslation.named("Linen shirt"),
                        CatalogLanguage.PL, ProductTranslation.named("Koszula lniana")),
                List.of(new ColorSwatchView(COLOR, "NAVY", "Granatowy", "#1C2340")),
                List.of(new AdminProductView.Variant(variant, COLOR, SIZE, "M", "LS001-NAV-M", "5901234123457", true)),
                List.of(new ImageView(image, COLOR, "products/p/i.webp", 10, 20)), now, now, 4));

        mvc.perform(get("/api/v1/admin/products").param("q", "len").param("status", "DRAFT")
                        .param("categoryId", CATEGORY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].missingLanguages[0]").value("cs"))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].updatedAt").value("2026-10-05T10:00:00Z"))
                .andExpect(jsonPath("$.items[0].cover").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.totalItems").value(1));
        verify(queries).products(new AdminProductFilter("len", ProductStatus.DRAFT, CATEGORY), new PageQuery(0, 25));

        mvc.perform(get("/api/v1/admin/products/{id}", PRODUCT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.pl.name").value("Koszula lniana"))
                .andExpect(jsonPath("$.translations.en.name").value("Linen shirt"))
                .andExpect(jsonPath("$.variants[0].size").value("M"))
                .andExpect(jsonPath("$.variants[0].active").value(true))
                .andExpect(jsonPath("$.images[0].url").value("/api/v1/media/products/p/i.webp"))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void deletesProduct() throws Exception {
        mvc.perform(delete("/api/v1/admin/products/{id}", PRODUCT)).andExpect(status().isNoContent());
        verify(products).delete(ProductId.of(PRODUCT));

        mvc.perform(delete("/api/v1/admin/products/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
