package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.application.view.ProductLabel;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Collection;
import java.util.Map;

/** Port udostępniany innym modułom: etykiety produktów po id. Usunięte produkty są pomijane w wyniku. */
public interface ProductLabelQuery {

    Map<ProductId, ProductLabel> labels(Collection<ProductId> productIds, CatalogLanguage language);
}
