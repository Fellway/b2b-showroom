package pl.wizytowka.catalog.application.view;

import java.util.UUID;

public record CategoryView(UUID id, UUID parentId, String slug, String name, String description, int sortOrder) {
}
