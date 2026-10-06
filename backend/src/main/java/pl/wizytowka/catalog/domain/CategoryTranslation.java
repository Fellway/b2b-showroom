package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;

public record CategoryTranslation(String name, String description) {

    public CategoryTranslation {
        name = Guard.requiredText(name, 120, "catalog.category.name");
        description = Guard.optionalText(description, 5_000, "catalog.category.description");
    }
}
