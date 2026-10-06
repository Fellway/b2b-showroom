package pl.wizytowka.catalog.adapter.in.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.PageResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.PublicCatalogResponses.CategoryResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.PublicCatalogResponses.FacetsResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.PublicCatalogResponses.ProductCardResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.PublicCatalogResponses.ProductDetailResponse;
import pl.wizytowka.catalog.application.port.in.BrowseCatalogUseCase;
import pl.wizytowka.catalog.application.view.CatalogFilter;
import pl.wizytowka.catalog.application.view.CatalogSort;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Publiczny katalog / lookbook. Odpowiedzi zależą od języka, dlatego mają {@code Vary: Accept-Language}
 * i krótki czas cache (katalog zmienia się rzadko, ale zmiany w panelu mają być szybko widoczne).
 */
@RestController
@RequestMapping("/api/v1/catalog")
class PublicCatalogController {

    private static final CacheControl CACHE = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();

    private final BrowseCatalogUseCase catalog;

    PublicCatalogController(BrowseCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    /**
     * @param color    id kolorów (parametr wielokrotny) — produkty dostępne w którymkolwiek z nich
     * @param size     id rozmiarów (parametr wielokrotny)
     * @param pageSize rozmiar strony (nazwa {@code size} jest zajęta przez filtr rozmiaru)
     */
    @GetMapping("/products")
    ResponseEntity<PageResponse<ProductCardResponse>> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Set<UUID> color,
            @RequestParam(required = false) Set<UUID> size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(defaultValue = "RECOMMENDED") CatalogSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @RequestedLanguage CatalogLanguage language) {
        CatalogFilter filter = new CatalogFilter(blankToNull(category), color, size, q, featured, sort);
        PageResponse<ProductCardResponse> body = PageResponse.of(
                catalog.products(filter, language, new PageQuery(page, pageSize)), ProductCardResponse::of);
        return cached(body);
    }

    @GetMapping("/products/{slug}")
    ResponseEntity<ProductDetailResponse> product(@PathVariable String slug, @RequestedLanguage CatalogLanguage language) {
        return cached(ProductDetailResponse.of(catalog.product(slug, language)));
    }

    @GetMapping("/categories")
    ResponseEntity<List<CategoryResponse>> categories(@RequestedLanguage CatalogLanguage language) {
        return cached(catalog.categories(language).stream().map(CategoryResponse::of).toList());
    }

    @GetMapping("/facets")
    ResponseEntity<FacetsResponse> facets(@RequestedLanguage CatalogLanguage language) {
        return cached(FacetsResponse.of(catalog.facets(language)));
    }

    private static <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .body(body);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
