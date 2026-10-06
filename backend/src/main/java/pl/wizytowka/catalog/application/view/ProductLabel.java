package pl.wizytowka.catalog.application.view;

import pl.wizytowka.catalog.domain.ProductStatus;

import java.util.UUID;

/** Krótki opis produktu do raportów (np. rankingi w analityce) — dla dowolnego statusu. */
public record ProductLabel(UUID id, String slug, String styleCode, String name, ProductStatus status) {
}
