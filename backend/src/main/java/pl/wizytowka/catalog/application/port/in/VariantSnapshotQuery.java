package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.application.view.VariantSnapshot;
import pl.wizytowka.catalog.domain.VariantId;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Collection;
import java.util.List;

/** Port udostępniany innym modułom (np. zestawieniom PDF): migawki wariantów możliwych do wyboru. */
public interface VariantSnapshotQuery {

    /**
     * Zwraca migawki w kolejności {@code variantIds}.
     * @throws pl.wizytowka.shared.domain.BusinessRuleViolationException (code {@code catalog.variant.not-selectable})
     *         gdy którykolwiek wariant nie istnieje, jest nieaktywny lub jego produkt nie jest opublikowany
     */
    List<VariantSnapshot> selectableVariants(Collection<VariantId> variantIds, CatalogLanguage language);
}
