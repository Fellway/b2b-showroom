package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.Guard;

public record ProductTranslation(String name, String description, String materialComposition, String careInstructions) {

    public ProductTranslation {
        name = Guard.requiredText(name, 200, "catalog.product.name");
        description = Guard.optionalText(description, 10_000, "catalog.product.description");
        materialComposition = Guard.optionalText(materialComposition, 500, "catalog.product.material");
        careInstructions = Guard.optionalText(careInstructions, 2_000, "catalog.product.care");
    }

    public static ProductTranslation named(String name) {
        return new ProductTranslation(name, null, null, null);
    }
}
