package pl.wizytowka.legal.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.legal.application.port.out.LegalDocumentRepository;
import pl.wizytowka.legal.application.view.AdminLegalOverview;
import pl.wizytowka.legal.application.view.LegalRequirement;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.ResourceConflictException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Szkic → publikacja → wymagania wobec kupców — bez Springa i bazy. */
class LegalDocumentServiceTest {

    static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    final InMemoryDocuments documents = new InMemoryDocuments();
    LegalDocumentService service;

    @BeforeEach
    void setUp() {
        service = new LegalDocumentService(documents, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static Map<CatalogLanguage, LegalText> texts(String suffix) {
        Map<CatalogLanguage, LegalText> texts = new EnumMap<>(CatalogLanguage.class);
        for (CatalogLanguage language : CatalogLanguage.values()) {
            texts.put(language, new LegalText("Regulamin " + language.code(), "Treść " + suffix, "Zmiana " + suffix));
        }
        return texts;
    }

    @Test
    void templateIsPublishedOnlyIntoAnEmptyDocument() {
        assertThat(service.publishTemplateIfEmpty(LegalDocumentType.TERMS, texts("v1"))).isTrue();
        assertThat(service.publishTemplateIfEmpty(LegalDocumentType.TERMS, texts("again"))).isFalse();

        assertThat(service.current(LegalDocumentType.TERMS, CatalogLanguage.CS)).get().satisfies(view -> {
            assertThat(view.versionNumber()).isEqualTo(1);
            assertThat(view.title()).isEqualTo("Regulamin cs");
            assertThat(view.language()).isEqualTo(CatalogLanguage.CS);
            assertThat(view.acceptanceRequired()).isTrue();
        });
        assertThat(service.current(LegalDocumentType.PRIVACY_POLICY, CatalogLanguage.PL)).isEmpty();
        assertThat(service.requirements()).singleElement()
                .extracting(LegalRequirement::type).isEqualTo(LegalDocumentType.TERMS);
    }

    @Test
    void draftCopiesCurrentTextWithoutChangeSummaryAndPublishesAsNextNumber() {
        service.publishTemplateIfEmpty(LegalDocumentType.TERMS, texts("v1"));
        LegalDocumentVersionId draftId = service.createDraft(LegalDocumentType.TERMS);

        assertThat(service.version(draftId).texts().get(CatalogLanguage.PL))
                .isEqualTo(new LegalText("Regulamin pl", "Treść v1", null));
        assertThatThrownBy(() -> service.createDraft(LegalDocumentType.TERMS))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("code", "legal.document.draft-exists");
        assertThat(service.current(LegalDocumentType.TERMS, CatalogLanguage.PL)).get()
                .extracting(v -> v.body()).as("szkic nie jest widoczny publicznie").isEqualTo("Treść v1");

        assertThatThrownBy(() -> service.updateDraft(draftId, 7, texts("v2")))
                .isInstanceOf(ConcurrentModificationException.class);
        service.updateDraft(draftId, 0, texts("v2"));
        UUID admin = UUID.randomUUID();
        assertThatThrownBy(() -> service.publish(draftId, 0, false, admin)).as("publikacja nieaktualnego szkicu")
                .isInstanceOf(ConcurrentModificationException.class);
        service.publish(draftId, 1, false, admin);

        assertThat(service.current(LegalDocumentType.TERMS, CatalogLanguage.PL)).get().satisfies(view -> {
            assertThat(view.versionNumber()).isEqualTo(2);
            assertThat(view.body()).isEqualTo("Treść v2");
            assertThat(view.changeSummary()).isEqualTo("Zmiana v2");
            assertThat(view.acceptanceRequired()).isFalse();
        });
        assertThat(service.requirement(LegalDocumentType.TERMS)).get().satisfies(requirement -> {
            assertThat(requirement.currentVersionNumber()).isEqualTo(2);
            assertThat(requirement.minimumVersionNumber()).as("poprawka redakcyjna nie podnosi minimum").isEqualTo(1);
            assertThat(requirement.blocking()).isTrue();
        });

        AdminLegalOverview overview = service.overview().getFirst();
        assertThat(overview.type()).isEqualTo(LegalDocumentType.TERMS);
        assertThat(overview.draft()).isNull();
        assertThat(overview.published()).extracting(s -> s.versionNumber()).containsExactly(2, 1);
    }

    @Test
    void publishedVersionsCannotBeDeletedAndOldVersionsStayReadable() {
        service.publishTemplateIfEmpty(LegalDocumentType.PRIVACY_POLICY, texts("v1"));
        UUID v1 = service.current(LegalDocumentType.PRIVACY_POLICY, CatalogLanguage.PL).orElseThrow().versionId();
        LegalDocumentVersionId draftId = service.createDraft(LegalDocumentType.PRIVACY_POLICY);
        service.publish(draftId, 0, true, UUID.randomUUID());

        assertThatThrownBy(() -> service.deleteDraft(LegalDocumentVersionId.of(v1)))
                .hasFieldOrPropertyWithValue("code", "legal.document.published-immutable");
        assertThat(service.publishedVersion(v1, CatalogLanguage.EN)).get()
                .satisfies(view -> assertThat(view.versionNumber()).isEqualTo(1));
        assertThat(service.requirement(LegalDocumentType.PRIVACY_POLICY)).get().satisfies(requirement -> {
            assertThat(requirement.minimumVersionNumber()).as("o każdej nowej wersji polityki informujemy").isEqualTo(2);
            assertThat(requirement.blocking()).isFalse();
        });

        LegalDocumentVersionId another = service.createDraft(LegalDocumentType.PRIVACY_POLICY);
        service.deleteDraft(another);
        assertThat(service.publishedVersion(another.value(), CatalogLanguage.PL)).isEmpty();
    }

    /** Repozytorium w pamięci (kopia przy zapisie — jak baza). */
    static final class InMemoryDocuments implements LegalDocumentRepository {
        final Map<LegalDocumentVersionId, LegalDocumentVersion> store = new LinkedHashMap<>();

        @Override
        public Optional<LegalDocumentVersion> findById(LegalDocumentVersionId id) {
            return Optional.ofNullable(store.get(id)).map(InMemoryDocuments::copy);
        }

        @Override
        public List<LegalDocumentVersion> findPublished(LegalDocumentType type) {
            return store.values().stream().filter(v -> v.type() == type && v.isPublished())
                    .sorted(Comparator.comparing(LegalDocumentVersion::versionNumber).reversed())
                    .map(InMemoryDocuments::copy).toList();
        }

        @Override
        public Optional<LegalDocumentVersion> findLatestPublished(LegalDocumentType type) {
            return findPublished(type).stream().findFirst();
        }

        @Override
        public Optional<LegalDocumentVersion> findDraft(LegalDocumentType type) {
            return store.values().stream().filter(v -> v.type() == type && v.isDraft()).findFirst()
                    .map(InMemoryDocuments::copy);
        }

        @Override
        public boolean existsAny(LegalDocumentType type) {
            return store.values().stream().anyMatch(v -> v.type() == type);
        }

        @Override
        public void save(LegalDocumentVersion version) {
            LegalDocumentVersion existing = store.get(version.id());
            if (existing != null && existing.version() != version.version()) {
                throw new ConcurrentModificationException("data.concurrent-modification", "stale");
            }
            store.put(version.id(), LegalDocumentVersion.restore(version.id(), version.type(), version.status(),
                    version.versionNumber(), version.texts(), version.acceptanceRequired(), version.publishedAt(),
                    version.publishedBy(), version.createdAt(), existing == null ? 0 : version.version() + 1));
        }

        @Override
        public void delete(LegalDocumentVersion version) {
            store.remove(version.id());
        }

        private static LegalDocumentVersion copy(LegalDocumentVersion v) {
            return LegalDocumentVersion.restore(v.id(), v.type(), v.status(), v.versionNumber(), v.texts(),
                    v.acceptanceRequired(), v.publishedAt(), v.publishedBy(), v.createdAt(), v.version());
        }
    }
}
