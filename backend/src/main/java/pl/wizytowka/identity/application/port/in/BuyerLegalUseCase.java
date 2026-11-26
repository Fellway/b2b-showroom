package pl.wizytowka.identity.application.port.in;

import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.List;
import java.util.UUID;

/**
 * Dokumenty prawne z perspektywy kupca (D25): co musi zaakceptować (nowy regulamin), o czym go informujemy (nowa
 * polityka prywatności) i historia złożonych oświadczeń.
 */
public interface BuyerLegalUseCase {

    BuyerLegalStatus status(BuyerAccountId buyer);

    /**
     * Akceptacja / przyjęcie do wiadomości obowiązujących wersji dokumentów. Ponowna akceptacja tej samej wersji
     * niczego nie zmienia.
     *
     * @param versionIds wersje widziane przez kupca — muszą być obowiązujące
     * @throws pl.wizytowka.shared.domain.ResourceConflictException gdy w międzyczasie opublikowano nowszą wersję
     */
    void accept(BuyerAccountId buyer, List<UUID> versionIds, CatalogLanguage language);

    /** Brama cen i zestawień: {@code true}, gdy kupiec nie zaakceptował wymaganej wersji regulaminu. */
    boolean mustAcceptTerms(UUID buyerAccountId);

    record BuyerLegalStatus(List<PendingDocument> pending, List<BuyerConsentView> history) {

        public boolean blocked() {
            return pending.stream().anyMatch(PendingDocument::blocking);
        }
    }

    /**
     * @param blocking {@code true} — bez akceptacji brak dostępu do cen i zestawień (regulamin); {@code false} —
     *                 informacja o nowej wersji (polityka prywatności)
     * @param firstTime kupiec nie zaakceptował dotąd żadnej wersji tego dokumentu (np. konto sprzed rejestru zgód)
     */
    record PendingDocument(LegalDocumentType type, UUID versionId, int versionNumber, boolean blocking,
                           boolean firstTime) {
    }
}
