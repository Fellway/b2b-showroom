package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record CategoryTranslationEmbeddable(
        @Column(name = "name", nullable = false, length = 120) String name,
        @Column(name = "description") String description) {
}
