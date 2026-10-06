package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.Translations;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional
class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository categories;

    CategoryPersistenceAdapter(CategoryJpaRepository categories) {
        this.categories = categories;
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return categories.findById(id.value()).map(CategoryPersistenceAdapter::toDomain);
    }

    @Override
    public void save(Category category) {
        CategoryJpaEntity entity = categories.findById(category.id().value()).orElse(null);
        if (entity == null) {
            entity = new CategoryJpaEntity(category.id().value());
            apply(category, entity);
            categories.save(entity);
            return;
        }
        if (!Objects.equals(entity.getVersion(), category.version())) {
            throw new ConcurrentModificationException("catalog.concurrent-modification",
                    "Category was modified by someone else; reload and try again");
        }
        apply(category, entity);
    }

    @Override
    public void delete(Category category) {
        categories.findById(category.id().value()).ifPresent(categories::delete);
    }

    @Override
    public boolean existsById(CategoryId id) {
        return categories.existsById(id.value());
    }

    @Override
    public boolean existsBySlug(String slug, CategoryId excluded) {
        return excluded == null ? categories.existsBySlug(slug) : categories.existsBySlugAndIdNot(slug, excluded.value());
    }

    @Override
    public boolean hasChildren(CategoryId id) {
        return categories.existsByParentId(id.value());
    }

    static Category toDomain(CategoryJpaEntity entity) {
        return Category.restore(CategoryId.of(entity.getId()), new CategoryDetails(
                entity.getParentId() == null ? null : CategoryId.of(entity.getParentId()),
                entity.getSlug(), entity.getSortOrder(), entity.isVisible(),
                Translations.of(translations(entity))), entity.getVersion());
    }

    static Map<CatalogLanguage, CategoryTranslation> translations(CategoryJpaEntity entity) {
        Map<CatalogLanguage, CategoryTranslation> result = new EnumMap<>(CatalogLanguage.class);
        entity.getTranslations().forEach((language, t) -> result.put(language, new CategoryTranslation(t.name(), t.description())));
        return result;
    }

    private static void apply(Category category, CategoryJpaEntity entity) {
        CategoryDetails details = category.details();
        entity.update(details.parentId() == null ? null : details.parentId().value(),
                details.slug(), details.sortOrder(), details.visible());
        Map<CatalogLanguage, CategoryTranslationEmbeddable> current = entity.getTranslations();
        Map<CatalogLanguage, CategoryTranslation> desired = details.translations().asMap();
        current.keySet().retainAll(desired.keySet());
        desired.forEach((language, t) -> {
            CategoryTranslationEmbeddable value = new CategoryTranslationEmbeddable(t.name(), t.description());
            if (!value.equals(current.get(language))) {
                current.put(language, value);
            }
        });
    }
}
