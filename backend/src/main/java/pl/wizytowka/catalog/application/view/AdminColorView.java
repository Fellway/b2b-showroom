package pl.wizytowka.catalog.application.view;

import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.Map;
import java.util.UUID;

public record AdminColorView(UUID id, String code, String hex, int sortOrder, Map<CatalogLanguage, String> names,
                             long productCount, long version) {
}
