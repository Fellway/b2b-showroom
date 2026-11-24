package pl.wizytowka.catalog.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.catalog.application.port.in.BrowseCatalogUseCase;
import pl.wizytowka.catalog.application.view.CatalogFacetsView;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CatalogSort;
import pl.wizytowka.catalog.application.view.CategoryView;
import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.ImageView;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.catalog.application.view.ProductCardView;
import pl.wizytowka.catalog.application.view.ProductDetailView;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class PublicCatalogControllerTest {

    static final UUID PRODUCT_ID = UUID.randomUUID();
    static final UUID COLOR_ID = UUID.randomUUID();
    static final UUID IMAGE_ID = UUID.randomUUID();
    static final PriceView PRICE = new PriceView(new BigDecimal("149.00"), new BigDecimal("349.99"), "PLN", new BigDecimal("2.35"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BrowseCatalogUseCase catalog;

    @Test
    void listsProductsWithFiltersPaginationAndCacheHeaders() throws Exception {
        UUID sizeA = UUID.randomUUID();
        UUID sizeB = UUID.randomUUID();
        ProductCardView card = new ProductCardView(PRODUCT_ID, "koszula-lniana", "LS-001", "Koszula lniana", "koszule",
                PRICE, true, new ImageView(IMAGE_ID, null, "products/p1/i1.webp", 1600, 2000),
                List.of(new ColorSwatchView(COLOR_ID, "NAVY", "Granatowy", "#1C2340")), List.of("S", "M"));
        when(catalog.products(any(), any(), any())).thenReturn(new PageResult<>(List.of(card), 1, 10, 11));

        mvc.perform(get("/api/v1/catalog/products")
                        .param("category", "koszule")
                        .param("color", COLOR_ID.toString())
                        .param("size", sizeA.toString(), sizeB.toString())
                        .param("q", "len")
                        .param("featured", "true")
                        .param("sort", "PRICE_ASC")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=60, public"))
                .andExpect(header().string("Vary", "Accept-Language"))
                .andExpect(jsonPath("$.items[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].price").doesNotExist())
                .andExpect(jsonPath("$.items[0].cover.url").value("/api/v1/media/products/p1/i1.webp"))
                .andExpect(jsonPath("$.items[0].colors[0].id").value(COLOR_ID.toString()))
                .andExpect(jsonPath("$.items[0].sizes[1]").value("M"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<CatalogFilter> filter = ArgumentCaptor.forClass(CatalogFilter.class);
        verify(catalog).products(filter.capture(), eq(CatalogLanguage.PL), eq(new PageQuery(1, 10)));
        assertThat(filter.getValue()).isEqualTo(
                new CatalogFilter("koszule", Set.of(COLOR_ID), Set.of(sizeA, sizeB), "len", true, CatalogSort.PRICE_ASC));
    }

    @Test
    void appliesDefaultsWhenNoParametersGiven() throws Exception {
        when(catalog.products(any(), any(), any())).thenReturn(new PageResult<>(List.of(), 0, 24, 0));

        mvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk());

        verify(catalog).products(CatalogFilter.none(), CatalogLanguage.PL, new PageQuery(0, 24));
    }

    @Test
    void resolvesLanguageFromQueryParameterThenAcceptLanguage() throws Exception {
        when(catalog.categories(any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/catalog/categories").param("lang", "uk").header("Accept-Language", "cs"))
                .andExpect(status().isOk());
        verify(catalog).categories(CatalogLanguage.UK);

        mvc.perform(get("/api/v1/catalog/categories").header("Accept-Language", "de-DE,cs;q=0.8,en;q=0.5"))
                .andExpect(status().isOk());
        verify(catalog).categories(CatalogLanguage.CS);

        mvc.perform(get("/api/v1/catalog/categories").header("Accept-Language", "de-DE"))
                .andExpect(status().isOk());
        verify(catalog).categories(CatalogLanguage.PL);
    }

    @Test
    void rejectsUnsupportedLanguageParameter() throws Exception {
        mvc.perform(get("/api/v1/catalog/categories").param("lang", "de"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("i18n.unsupported-language"));
        verifyNoInteractions(catalog);
    }

    @Test
    void rejectsTooLargePageAndUnknownSort() throws Exception {
        mvc.perform(get("/api/v1/catalog/products").param("pageSize", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("page.size-invalid"));

        mvc.perform(get("/api/v1/catalog/products").param("sort", "DROP TABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.400"));

        mvc.perform(get("/api/v1/catalog/products").param("color", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }

    @Test
    void returnsProductDetailInRequestedLanguage() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductDetailView view = new ProductDetailView(PRODUCT_ID, "koszula-lniana", "LS-001", "Linen shirt",
                "Relaxed fit", "100% linen", null,
                new ProductDetailView.CategoryRef(categoryId, "shirts", "Shirts"), PRICE,
                List.of(new ProductDetailView.ColorOption(COLOR_ID, "NAVY", "Navy", "#1C2340",
                        List.of(new ProductDetailView.VariantOption(variantId, "M", "LS001-NAV-M", "5901234123457")))),
                List.of(new ImageView(IMAGE_ID, COLOR_ID, "products/p1/i1.webp", null, null)));
        when(catalog.product("koszula-lniana", CatalogLanguage.EN)).thenReturn(view);

        mvc.perform(get("/api/v1/catalog/products/koszula-lniana").header("Accept-Language", "en-GB"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "Accept-Language"))
                .andExpect(jsonPath("$.name").value("Linen shirt"))
                .andExpect(jsonPath("$.category.slug").value("shirts"))
                // ceny i EAN tylko dla zalogowanych kupców (D23)
                .andExpect(jsonPath("$.price").doesNotExist())
                .andExpect(jsonPath("$.colors[0].variants[0].id").value(variantId.toString()))
                .andExpect(jsonPath("$.colors[0].variants[0].size").value("M"))
                .andExpect(jsonPath("$.colors[0].variants[0].sku").value("LS001-NAV-M"))
                .andExpect(jsonPath("$.colors[0].variants[0].ean").doesNotExist())
                .andExpect(jsonPath("$.images[0].colorId").value(COLOR_ID.toString()))
                .andExpect(jsonPath("$.images[0].url").value("/api/v1/media/products/p1/i1.webp"));
    }

    @Test
    void mapsMissingProductTo404WithCode() throws Exception {
        when(catalog.product(any(), any()))
                .thenThrow(new ResourceNotFoundException("catalog.product.not-found", "Product not found"));

        mvc.perform(get("/api/v1/catalog/products/nie-ma"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("catalog.product.not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returnsCategoriesAndFacets() throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID sizeId = UUID.randomUUID();
        when(catalog.categories(CatalogLanguage.PL))
                .thenReturn(List.of(new CategoryView(categoryId, null, "koszule", "Koszule", null, 1)));
        when(catalog.facets(CatalogLanguage.PL)).thenReturn(new CatalogFacetsView(
                List.of(new CatalogFacetsView.ColorFacet(COLOR_ID, "NAVY", "Granatowy", "#1C2340", 4)),
                List.of(new CatalogFacetsView.SizeFacet(sizeId, "M", 7))));

        mvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].name").value("Koszule"))
                .andExpect(jsonPath("$[0].parentId").value(org.hamcrest.Matchers.nullValue()));

        mvc.perform(get("/api/v1/catalog/facets"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=60, public"))
                .andExpect(jsonPath("$.colors[0].productCount").value(4))
                .andExpect(jsonPath("$.sizes[0].code").value("M"))
                .andExpect(jsonPath("$.sizes[0].productCount").value(7));
    }
}
