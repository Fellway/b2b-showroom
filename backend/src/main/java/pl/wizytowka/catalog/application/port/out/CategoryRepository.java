package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryId;

import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(CategoryId id);

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(Category category);

    void delete(Category category);

    boolean existsById(CategoryId id);

    boolean existsBySlug(String slug, CategoryId excluded);

    boolean hasChildren(CategoryId id);
}
