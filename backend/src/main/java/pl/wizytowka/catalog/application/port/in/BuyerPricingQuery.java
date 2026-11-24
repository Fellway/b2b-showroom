package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.application.view.ProductPricingView;
import pl.wizytowka.catalog.domain.ProductId;

import java.util.Collection;
import java.util.List;

/** Ceny i kody EAN dla zalogowanego kupca (D23) — dociągane do publicznych stron katalogu po stronie przeglądarki. */
public interface BuyerPricingQuery {

    int MAX_PRODUCTS = 100;

    /**
     * Dane tylko produktów widocznych w publicznym katalogu (opublikowane, w widocznej kategorii); pozostałe id
     * są pomijane, kolejność jak w żądaniu.
     * @throws pl.wizytowka.shared.domain.InvalidValueException gdy żądanie obejmuje więcej niż {@link #MAX_PRODUCTS} produktów
     */
    List<ProductPricingView> pricing(Collection<ProductId> productIds);
}
