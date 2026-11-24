package pl.wizytowka.catalog.adapter.in.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.PriceResponse;
import pl.wizytowka.catalog.application.port.in.BuyerPricingQuery;
import pl.wizytowka.catalog.application.view.ProductPricingView;
import pl.wizytowka.catalog.domain.ProductId;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ceny i kody EAN dla zalogowanego kupca (rola BUYER — reguła w SecurityConfig, D23). Publiczne strony katalogu
 * zostają współdzielone i cache'owane, a przeglądarka kupca dociąga te dane osobnym żądaniem — dlatego odpowiedź
 * nigdy nie trafia do współdzielonego cache.
 */
@RestController
@RequestMapping("/api/v1/buyer/catalog")
class BuyerCatalogController {

    private final BuyerPricingQuery pricing;

    BuyerCatalogController(BuyerPricingQuery pricing) {
        this.pricing = pricing;
    }

    /** @param product id produktów (parametr wielokrotny, maks. {@value BuyerPricingQuery#MAX_PRODUCTS}) */
    @GetMapping("/pricing")
    ResponseEntity<List<ProductPricingResponse>> pricing(@RequestParam(name = "product") Set<UUID> product) {
        List<ProductPricingResponse> body = pricing.pricing(product.stream().map(ProductId::of).toList()).stream()
                .map(ProductPricingResponse::of)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
    }

    record ProductPricingResponse(UUID productId, PriceResponse price, List<VariantCodeResponse> variants) {

        static ProductPricingResponse of(ProductPricingView view) {
            return new ProductPricingResponse(view.productId(), PriceResponse.of(view.price()),
                    view.variants().stream().map(v -> new VariantCodeResponse(v.variantId(), v.sku(), v.ean())).toList());
        }
    }

    record VariantCodeResponse(UUID variantId, String sku, String ean) {
    }
}
