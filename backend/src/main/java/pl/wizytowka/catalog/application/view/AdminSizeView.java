package pl.wizytowka.catalog.application.view;

import java.util.UUID;

public record AdminSizeView(UUID id, String code, int sortOrder, long variantCount, long version) {
}
