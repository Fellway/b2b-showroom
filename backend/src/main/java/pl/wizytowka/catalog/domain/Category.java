package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Objects;

/**
 * Kategoria katalogu (drzewo). Brak cykli w drzewie sprawdza warstwa aplikacji,
 * bo wymaga to znajomości przodków spoza agregatu.
 */
public final class Category {

    private final CategoryId id;
    private CategoryDetails details;
    private final long version;

    private Category(CategoryId id, CategoryDetails details, long version) {
        this.id = Objects.requireNonNull(id);
        this.version = version;
        update(details);
    }

    public static Category create(CategoryDetails details) {
        return new Category(CategoryId.newId(), details, 0);
    }

    public static Category restore(CategoryId id, CategoryDetails details, long version) {
        return new Category(id, details, version);
    }

    public void update(CategoryDetails details) {
        Objects.requireNonNull(details, "details");
        if (id.equals(details.parentId())) {
            throw new InvalidValueException("catalog.category.own-parent", "Category cannot be its own parent");
        }
        this.details = details;
    }

    public CategoryId id() {
        return id;
    }

    public CategoryDetails details() {
        return details;
    }

    public CategoryId parentId() {
        return details.parentId();
    }

    public long version() {
        return version;
    }
}
