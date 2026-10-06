package pl.wizytowka.catalog.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.Translations;
import pl.wizytowka.support.PersistenceTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PersistenceTest
class DictionaryPersistenceAdaptersTest {

    @Autowired
    CategoryRepository categories;
    @Autowired
    ColorRepository colors;
    @Autowired
    SizeRepository sizes;
    @Autowired
    EntityManager entityManager;

    @Test
    void categoryRoundTripTranslationsHierarchyAndVersioning() {
        Category root = Category.create(new CategoryDetails(null, "odziez", 0, true,
                Translations.ofDefault(new CategoryTranslation("Odzież", null))));
        categories.save(root);
        Category child = Category.create(new CategoryDetails(root.id(), "koszule", 3, false,
                Translations.of(Map.of(CatalogLanguage.PL, new CategoryTranslation("Koszule", "Opis"),
                        CatalogLanguage.UK, new CategoryTranslation("Сорочки", null)))));
        categories.save(child);
        flushAndClear();

        Category loaded = categories.findById(child.id()).orElseThrow();
        assertThat(loaded.details()).isEqualTo(child.details());
        assertThat(loaded.version()).isZero();
        assertThat(categories.hasChildren(root.id())).isTrue();
        assertThat(categories.hasChildren(child.id())).isFalse();
        assertThat(categories.existsBySlug("koszule", null)).isTrue();
        assertThat(categories.existsBySlug("koszule", child.id())).isFalse();
        assertThat(categories.existsById(root.id())).isTrue();

        // zmiana wyłącznie tłumaczeń (element collection) też podbija wersję
        loaded.update(new CategoryDetails(root.id(), "koszule", 3, false,
                Translations.ofDefault(new CategoryTranslation("Koszule męskie", null))));
        categories.save(loaded);
        flushAndClear();

        Category reloaded = categories.findById(child.id()).orElseThrow();
        assertThat(reloaded.version()).isEqualTo(1);
        assertThat(reloaded.details().translations().asMap()).containsOnlyKeys(CatalogLanguage.PL);

        assertThatThrownBy(() -> categories.save(loaded)).isInstanceOf(ConcurrentModificationException.class);

        categories.delete(reloaded);
        flushAndClear();
        assertThat(categories.findById(child.id())).isEmpty();
    }

    @Test
    void colorAndSizeRoundTrip() {
        Color color = Color.create(new ColorDetails("navy", "#1c2340", 2,
                Translations.of(Map.of(CatalogLanguage.PL, "Granatowy", CatalogLanguage.CS, "Námořnická modrá"))));
        colors.save(color);
        Size size = Size.create("xl", 50);
        sizes.save(size);
        flushAndClear();

        Color loadedColor = colors.findById(color.id()).orElseThrow();
        assertThat(loadedColor.details()).isEqualTo(color.details());
        assertThat(colors.existsByCode("NAVY", null)).isTrue();
        assertThat(colors.existsByCode("NAVY", color.id())).isFalse();

        Size loadedSize = sizes.findById(size.id()).orElseThrow();
        assertThat(loadedSize.code()).isEqualTo("XL");
        assertThat(loadedSize.sortOrder()).isEqualTo(50);
        assertThat(sizes.existsByCode("XL", null)).isTrue();

        loadedSize.update("XL", 55);
        sizes.save(loadedSize);
        flushAndClear();
        assertThat(sizes.findById(size.id()).orElseThrow().version()).isEqualTo(1);
        assertThatThrownBy(() -> sizes.save(loadedSize)).isInstanceOf(ConcurrentModificationException.class);

        colors.delete(loadedColor);
        flushAndClear();
        assertThat(colors.existsById(color.id())).isFalse();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
