package pl.wizytowka.catalog.adapter.out.persistence;

import pl.wizytowka.catalog.application.view.ColorSwatchView;
import pl.wizytowka.catalog.application.view.ImageView;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Słowniki katalogu (kategorie, kolory, rozmiary) wczytane raz na zapytanie. Są małe (dziesiątki-setki
 * wierszy), więc składanie widoków w pamięci jest tańsze niż wielokrotne JOIN-y z tłumaczeniami.
 */
final class CatalogDictionary {

    /** Zabezpieczenie przed uszkodzonym drzewem (cykl) — głębokość kategorii jest w praktyce kilkupoziomowa. */
    private static final int MAX_DEPTH = 64;

    private final Map<UUID, CategoryJpaEntity> categories;
    private final Map<UUID, ColorJpaEntity> colors;
    private final Map<UUID, SizeJpaEntity> sizes;
    private final Map<UUID, List<UUID>> childrenByParent;

    CatalogDictionary(List<CategoryJpaEntity> categories, List<ColorJpaEntity> colors, List<SizeJpaEntity> sizes) {
        this.categories = categories.stream().collect(Collectors.toMap(CategoryJpaEntity::getId, Function.identity()));
        this.colors = colors.stream().collect(Collectors.toMap(ColorJpaEntity::getId, Function.identity()));
        this.sizes = sizes.stream().collect(Collectors.toMap(SizeJpaEntity::getId, Function.identity()));
        this.childrenByParent = new HashMap<>();
        categories.stream()
                .filter(c -> c.getParentId() != null)
                .forEach(c -> childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId()));
    }

    // ------------------------------------------------------------------ kategorie

    Optional<CategoryJpaEntity> category(UUID id) {
        return Optional.ofNullable(categories.get(id));
    }

    Optional<CategoryJpaEntity> categoryBySlug(String slug) {
        return categories.values().stream().filter(c -> c.getSlug().equals(slug)).findFirst();
    }

    /** Kategorie widoczne publicznie: same oznaczone jako widoczne i z widocznymi wszystkimi przodkami. */
    Set<UUID> visibleCategoryIds() {
        return categories.keySet().stream().filter(this::isVisibleWithAncestors).collect(Collectors.toUnmodifiableSet());
    }

    private boolean isVisibleWithAncestors(UUID id) {
        UUID current = id;
        for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
            CategoryJpaEntity category = categories.get(current);
            if (category == null || !category.isVisible()) {
                return false;
            }
            current = category.getParentId();
        }
        return current == null;
    }

    /** Kategoria wraz ze wszystkimi potomkami. */
    Set<UUID> withDescendants(UUID rootId) {
        Set<UUID> result = new HashSet<>();
        List<UUID> pending = new ArrayList<>(List.of(rootId));
        while (!pending.isEmpty()) {
            UUID id = pending.removeLast();
            if (result.add(id)) {
                pending.addAll(childrenByParent.getOrDefault(id, List.of()));
            }
        }
        return result;
    }

    String categorySlug(UUID id) {
        return category(id).map(CategoryJpaEntity::getSlug).orElse(null);
    }

    String categoryName(UUID id, CatalogLanguage language) {
        return category(id).map(c -> resolve(c.getTranslations(), language).name()).orElse(null);
    }

    List<CategoryJpaEntity> categoriesSorted() {
        return categories.values().stream()
                .sorted(Comparator.comparingInt(CategoryJpaEntity::getSortOrder).thenComparing(CategoryJpaEntity::getSlug))
                .toList();
    }

    // ------------------------------------------------------------------ kolory i rozmiary

    ColorSwatchView swatch(UUID colorId, CatalogLanguage language) {
        ColorJpaEntity color = colors.get(colorId);
        if (color == null) {
            return new ColorSwatchView(colorId, null, null, null);
        }
        return new ColorSwatchView(colorId, color.getCode(), colorName(colorId, language), color.getHexValue());
    }

    String colorName(UUID colorId, CatalogLanguage language) {
        ColorJpaEntity color = colors.get(colorId);
        if (color == null) {
            return null;
        }
        String name = resolve(color.getNames(), language);
        return name != null ? name : color.getCode();
    }

    List<ColorJpaEntity> colorsSorted() {
        return colors.values().stream()
                .sorted(Comparator.comparingInt(ColorJpaEntity::getSortOrder).thenComparing(ColorJpaEntity::getCode))
                .toList();
    }

    String sizeCode(UUID sizeId) {
        SizeJpaEntity size = sizes.get(sizeId);
        return size == null ? null : size.getCode();
    }

    Comparator<UUID> sizeOrder() {
        return Comparator.comparingInt((UUID id) -> sizes.containsKey(id) ? sizes.get(id).getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(id -> Optional.ofNullable(sizeCode(id)).orElse(""));
    }

    List<SizeJpaEntity> sizesSorted() {
        return sizes.values().stream()
                .sorted(Comparator.comparingInt(SizeJpaEntity::getSortOrder).thenComparing(SizeJpaEntity::getCode))
                .toList();
    }

    // ------------------------------------------------------------------ pomocnicze

    static <T> T resolve(Map<CatalogLanguage, T> translations, CatalogLanguage language) {
        T exact = translations.get(language);
        return exact != null ? exact : translations.get(CatalogLanguage.DEFAULT);
    }

    static ImageView image(ProductImageJpaEntity image) {
        return new ImageView(image.getId(), image.getColorId(), image.getStorageKey(), image.getWidthPx(), image.getHeightPx());
    }

    static List<ImageView> images(ProductJpaEntity product) {
        return sortedImages(product).stream().map(CatalogDictionary::image).toList();
    }

    static List<ProductImageJpaEntity> sortedImages(ProductJpaEntity product) {
        return product.getImages().stream().sorted(Comparator.comparingInt(ProductImageJpaEntity::getSortOrder)).toList();
    }

    static List<UUID> sortedColorIds(ProductJpaEntity product) {
        return product.getColors().stream()
                .sorted(Comparator.comparingInt(ProductColorJpaEntity::getSortOrder))
                .map(ProductColorJpaEntity::getColorId)
                .toList();
    }

    /** Pierwsze zdjęcie koloru, a w razie braku — główne zdjęcie produktu (jak {@code Product#coverImage}). */
    static Optional<ProductImageJpaEntity> coverImage(ProductJpaEntity product, UUID colorId) {
        List<ProductImageJpaEntity> images = sortedImages(product);
        return images.stream()
                .filter(image -> colorId != null && colorId.equals(image.getColorId()))
                .findFirst()
                .or(() -> images.stream().findFirst());
    }

    static <K> Map<K, Long> counts(List<Object[]> rows) {
        Map<K, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            @SuppressWarnings("unchecked")
            K key = (K) row[0];
            result.put(key, ((Number) row[1]).longValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /** Wzorzec LIKE z ucieczką znaków specjalnych (znak ucieczki: '!'). */
    static String containsPattern(String search) {
        String escaped = search.toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
