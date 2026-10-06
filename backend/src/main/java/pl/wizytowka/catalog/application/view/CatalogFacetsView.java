package pl.wizytowka.catalog.application.view;

import java.util.List;
import java.util.UUID;

/** Dostępne wartości filtrów wraz z liczbą opublikowanych produktów. */
public record CatalogFacetsView(List<ColorFacet> colors, List<SizeFacet> sizes) {

    public record ColorFacet(UUID colorId, String code, String name, String hex, long productCount) {
    }

    public record SizeFacet(UUID sizeId, String code, long productCount) {
    }
}
