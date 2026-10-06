package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;

public interface ManageCategoriesUseCase {

    CategoryId create(CategoryDetails details);

    void update(CategoryId id, long expectedVersion, CategoryDetails details);

    /** Dozwolone tylko dla kategorii bez podkategorii i bez produktów. */
    void delete(CategoryId id);
}
