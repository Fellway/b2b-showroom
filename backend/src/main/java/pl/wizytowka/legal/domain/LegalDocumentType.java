package pl.wizytowka.legal.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Rodzaje dokumentów prawnych showroomu (D25).
 *
 * <ul>
 *   <li>{@link #TERMS} — regulamin: umowa, którą kupiec <em>akceptuje</em>; istotna zmiana wymaga ponownej akceptacji
 *       przed dostępem do cen i zestawień.</li>
 *   <li>{@link #PRIVACY_POLICY} — polityka prywatności: obowiązek informacyjny (art. 13 RODO), nie zgoda; zmianę
 *       kupiec tylko przyjmuje do wiadomości.</li>
 * </ul>
 */
public enum LegalDocumentType {
    TERMS("terms"),
    PRIVACY_POLICY("privacy");

    private final String slug;

    LegalDocumentType(String slug) {
        this.slug = slug;
    }

    /** Krótka nazwa w adresach API i stron ({@code terms}, {@code privacy}). */
    public String slug() {
        return slug;
    }

    /** Czy nowa wersja może blokować dostęp do konta do czasu jej akceptacji. */
    public boolean canRequireAcceptance() {
        return this == TERMS;
    }

    public static Optional<LegalDocumentType> fromSlug(String slug) {
        return Arrays.stream(values()).filter(type -> type.slug.equals(slug)).findFirst();
    }
}
