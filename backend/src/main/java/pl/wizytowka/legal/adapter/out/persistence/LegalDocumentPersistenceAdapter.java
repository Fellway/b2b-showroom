package pl.wizytowka.legal.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.wizytowka.legal.application.port.out.LegalDocumentRepository;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalDocumentVersion.Status;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
class LegalDocumentPersistenceAdapter implements LegalDocumentRepository {

    private final LegalDocumentVersionJpaRepository repository;

    LegalDocumentPersistenceAdapter(LegalDocumentVersionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LegalDocumentVersion> findById(LegalDocumentVersionId id) {
        return repository.findById(id.value()).map(LegalDocumentPersistenceAdapter::toDomain);
    }

    @Override
    public List<LegalDocumentVersion> findPublished(LegalDocumentType type) {
        return repository.findByDocumentTypeAndStatusOrderByVersionNumberDesc(type, Status.PUBLISHED).stream()
                .map(LegalDocumentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<LegalDocumentVersion> findLatestPublished(LegalDocumentType type) {
        return repository.findFirstByDocumentTypeAndStatusOrderByVersionNumberDesc(type, Status.PUBLISHED)
                .map(LegalDocumentPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<LegalDocumentVersion> findDraft(LegalDocumentType type) {
        return repository.findFirstByDocumentTypeAndStatusOrderByVersionNumberDesc(type, Status.DRAFT)
                .map(LegalDocumentPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsAny(LegalDocumentType type) {
        return repository.existsByDocumentType(type);
    }

    @Override
    public void save(LegalDocumentVersion version) {
        LegalDocumentVersionJpaEntity entity = repository.findById(version.id().value())
                .map(existing -> {
                    Versions.require(version.version(), existing.version);
                    return existing;
                })
                .orElseGet(() -> newEntity(version));
        if (entity.status == Status.PUBLISHED) {
            // opublikowana treść jest niezmienna — chroni rejestr zgód także przed błędem w warstwie wyżej
            throw new IllegalStateException("Published legal document version " + version.id() + " is immutable");
        }
        entity.status = version.status();
        entity.versionNumber = version.versionNumber();
        entity.acceptanceRequired = version.acceptanceRequired();
        entity.publishedAt = version.publishedAt();
        entity.publishedBy = version.publishedBy();
        syncTexts(entity.texts, version.texts());
        repository.save(entity);
    }

    @Override
    public void delete(LegalDocumentVersion version) {
        repository.deleteById(version.id().value());
    }

    private static LegalDocumentVersionJpaEntity newEntity(LegalDocumentVersion version) {
        LegalDocumentVersionJpaEntity entity = new LegalDocumentVersionJpaEntity(version.id().value());
        entity.documentType = version.type();
        entity.status = Status.DRAFT;
        entity.createdAt = version.createdAt();
        entity.updatedAt = Instant.now();
        return entity;
    }

    /** Aktualizuje zarządzaną mapę w miejscu (bez podmiany kolekcji). */
    private static void syncTexts(Map<String, LegalDocumentVersionJpaEntity.Text> target,
                                  Map<CatalogLanguage, LegalText> source) {
        target.keySet().removeIf(code -> CatalogLanguage.fromCode(code).map(l -> !source.containsKey(l)).orElse(true));
        source.forEach((language, text) -> {
            LegalDocumentVersionJpaEntity.Text value =
                    new LegalDocumentVersionJpaEntity.Text(text.title(), text.body(), text.changeSummary());
            if (!value.equals(target.get(language.code()))) {
                target.put(language.code(), value);
            }
        });
    }

    private static LegalDocumentVersion toDomain(LegalDocumentVersionJpaEntity e) {
        Map<CatalogLanguage, LegalText> texts = new EnumMap<>(CatalogLanguage.class);
        e.texts.forEach((code, text) -> CatalogLanguage.fromCode(code)
                .ifPresent(language -> texts.put(language, new LegalText(text.title, text.body, text.changeSummary))));
        return LegalDocumentVersion.restore(LegalDocumentVersionId.of(e.id), e.documentType, e.status,
                e.versionNumber, texts, e.acceptanceRequired, e.publishedAt, e.publishedBy, e.createdAt, e.version);
    }
}
