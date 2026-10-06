package pl.wizytowka.catalog.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.shared.domain.ConcurrentModificationException;

import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional
class SizePersistenceAdapter implements SizeRepository {

    private final SizeJpaRepository sizes;

    SizePersistenceAdapter(SizeJpaRepository sizes) {
        this.sizes = sizes;
    }

    @Override
    public Optional<Size> findById(SizeId id) {
        return sizes.findById(id.value())
                .map(entity -> Size.restore(SizeId.of(entity.getId()), entity.getCode(), entity.getSortOrder(), entity.getVersion()));
    }

    @Override
    public void save(Size size) {
        SizeJpaEntity entity = sizes.findById(size.id().value()).orElse(null);
        if (entity == null) {
            entity = new SizeJpaEntity(size.id().value());
            entity.update(size.code(), size.sortOrder());
            sizes.save(entity);
            return;
        }
        if (!Objects.equals(entity.getVersion(), size.version())) {
            throw new ConcurrentModificationException("catalog.concurrent-modification",
                    "Size was modified by someone else; reload and try again");
        }
        entity.update(size.code(), size.sortOrder());
    }

    @Override
    public void delete(Size size) {
        sizes.findById(size.id().value()).ifPresent(sizes::delete);
    }

    @Override
    public boolean existsById(SizeId id) {
        return sizes.existsById(id.value());
    }

    @Override
    public boolean existsByCode(String code, SizeId excluded) {
        return excluded == null ? sizes.existsByCode(code) : sizes.existsByCodeAndIdNot(code, excluded.value());
    }
}
