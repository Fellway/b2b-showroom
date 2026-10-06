package pl.wizytowka.catalog.application.view;

import java.util.UUID;

public record ColorSwatchView(UUID colorId, String code, String name, String hex) {
}
