package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.Translations;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional
class ColorPersistenceAdapter implements ColorRepository {

    private final ColorJpaRepository colors;

    ColorPersistenceAdapter(ColorJpaRepository colors) {
        this.colors = colors;
    }

    @Override
    public Optional<Color> findById(ColorId id) {
        return colors.findById(id.value()).map(entity -> Color.restore(ColorId.of(entity.getId()),
                new ColorDetails(entity.getCode(), entity.getHexValue(), entity.getSortOrder(),
                        Translations.of(entity.getNames())),
                entity.getVersion()));
    }

    @Override
    public void save(Color color) {
        ColorJpaEntity entity = colors.findById(color.id().value()).orElse(null);
        if (entity == null) {
            entity = new ColorJpaEntity(color.id().value());
            apply(color, entity);
            colors.save(entity);
            return;
        }
        if (!Objects.equals(entity.getVersion(), color.version())) {
            throw new ConcurrentModificationException("catalog.concurrent-modification",
                    "Color was modified by someone else; reload and try again");
        }
        apply(color, entity);
    }

    @Override
    public void delete(Color color) {
        colors.findById(color.id().value()).ifPresent(colors::delete);
    }

    @Override
    public boolean existsById(ColorId id) {
        return colors.existsById(id.value());
    }

    @Override
    public boolean existsByCode(String code, ColorId excluded) {
        return excluded == null ? colors.existsByCode(code) : colors.existsByCodeAndIdNot(code, excluded.value());
    }

    private static void apply(Color color, ColorJpaEntity entity) {
        ColorDetails details = color.details();
        entity.update(details.code(), details.hexValue(), details.sortOrder());
        Map<CatalogLanguage, String> desired = details.translations().asMap();
        Map<CatalogLanguage, String> current = entity.getNames();
        current.keySet().retainAll(desired.keySet());
        desired.forEach((language, name) -> {
            if (!name.equals(current.get(language))) {
                current.put(language, name);
            }
        });
    }
}
