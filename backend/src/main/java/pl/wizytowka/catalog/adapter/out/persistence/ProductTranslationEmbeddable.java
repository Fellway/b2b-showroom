package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ProductTranslationEmbeddable(
        @Column(name = "name", nullable = false, length = 200) String name,
        @Column(name = "description") String description,
        @Column(name = "material_composition", length = 500) String materialComposition,
        @Column(name = "care_instructions") String careInstructions) {
}
