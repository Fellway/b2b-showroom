package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Zestawienie ofertowe (line sheet) wygenerowane przez zalogowanego kupca (D23). Treść jest niezmienna od chwili
 * utworzenia; zmienia się wyłącznie stan techniczny (dołączony plik PDF). Zestawień nie wysyłamy e-mailem —
 * kupiec pobiera PDF; dane kontaktowe są migawką danych konta z chwili generowania.
 *
 * <p>Zgoda na przetwarzanie danych (RODO) jest wymagana, gdy klient podał jakiekolwiek dane kontaktowe.</p>
 */
public final class LineSheet {

    public static final int MAX_ITEMS = 200;
    private static final Pattern REFERENCE = Pattern.compile("^LS-\\d{4}-\\d{6,16}$");

    private final LineSheetId id;
    private final UUID publicId;
    private final String referenceNumber;
    private final CatalogLanguage language;
    private final Currency currency;
    private final Recipient recipient;
    private final String message;
    private final Instant privacyAcceptedAt;
    private final UUID salesRepresentativeId;
    private final UUID visitorId;
    private final UUID buyerAccountId;
    private final List<LineSheetItem> items;
    private final Instant createdAt;
    private String pdfStorageKey;
    private Instant pdfGeneratedAt;
    private final long version;

    private LineSheet(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.publicId = Objects.requireNonNull(b.publicId, "publicId");
        this.referenceNumber = requireReference(b.referenceNumber);
        this.language = Objects.requireNonNull(b.language, "language");
        this.currency = Objects.requireNonNull(b.currency, "currency");
        this.recipient = Objects.requireNonNullElseGet(b.recipient, Recipient::anonymous);
        this.message = Guard.optionalText(b.message, 2000, "linesheet.message");
        this.privacyAcceptedAt = b.privacyAcceptedAt;
        this.salesRepresentativeId = b.salesRepresentativeId;
        this.visitorId = b.visitorId;
        this.buyerAccountId = b.buyerAccountId;
        this.items = List.copyOf(b.items);
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.pdfStorageKey = b.pdfStorageKey;
        this.pdfGeneratedAt = b.pdfGeneratedAt;
        this.version = b.version;
    }

    /**
     * Tworzy zestawienie z wybranych wariantów (w kolejności prezentacji).
     * @param referenceNumber numer nadany przez generator, np. LS-2026-000042
     * @param buyerAccountId  konto kupca, który wygenerował zestawienie (D23); {@code null} tylko w danych historycznych
     */
    public static LineSheet create(String referenceNumber, CatalogLanguage language, Recipient recipient, String message,
                                   boolean privacyAccepted, UUID salesRepresentativeId,
                                   UUID visitorId, UUID buyerAccountId, List<SelectedVariant> selections, Instant now) {
        Objects.requireNonNull(now, "now");
        Recipient effectiveRecipient = Objects.requireNonNullElseGet(recipient, Recipient::anonymous);
        if (selections == null || selections.isEmpty()) {
            throw new InvalidValueException("linesheet.items.empty", "Line sheet requires at least one item");
        }
        if (selections.size() > MAX_ITEMS) {
            throw new InvalidValueException("linesheet.items.too-many", "Line sheet can contain at most " + MAX_ITEMS + " items");
        }
        Set<UUID> variantIds = new HashSet<>();
        for (SelectedVariant selection : selections) {
            if (!variantIds.add(selection.variantId())) {
                throw new InvalidValueException("linesheet.items.duplicate", "Each variant can be selected only once");
            }
        }
        Currency currency = selections.getFirst().pricing().currency();
        if (selections.stream().anyMatch(s -> !s.pricing().currency().equals(currency))) {
            throw new BusinessRuleViolationException("linesheet.items.mixed-currency", "All items must share one currency");
        }
        String trimmedMessage = Guard.optionalText(message, 2000, "linesheet.message");
        if (effectiveRecipient.hasAnyData() && !privacyAccepted) {
            throw new InvalidValueException("linesheet.privacy.consent-required",
                    "Consent to personal data processing is required when contact details are provided");
        }
        List<LineSheetItem> items = new ArrayList<>(selections.size());
        for (int i = 0; i < selections.size(); i++) {
            SelectedVariant s = selections.get(i);
            items.add(new LineSheetItem(i + 1, s.productId(), s.variantId(), s.styleCode(), s.productName(),
                    s.colorName(), s.sizeCode(), s.sku(), s.ean(), s.pricing().wholesalePrice(),
                    s.pricing().retailPrice(), s.imageStorageKey(), s.quantity()));
        }
        Builder b = new Builder();
        b.id = LineSheetId.newId();
        b.publicId = UUID.randomUUID();
        b.referenceNumber = referenceNumber;
        b.language = language;
        b.currency = currency;
        b.recipient = effectiveRecipient;
        b.message = trimmedMessage;
        b.privacyAcceptedAt = privacyAccepted ? now : null;
        b.salesRepresentativeId = salesRepresentativeId;
        b.visitorId = visitorId;
        b.buyerAccountId = buyerAccountId;
        b.items = items;
        b.createdAt = now;
        return new LineSheet(b);
    }

    public static String formatReference(int year, long sequenceNumber) {
        return "LS-%04d-%06d".formatted(year, sequenceNumber);
    }

    // ------------------------------------------------------------------ PDF

    public void attachPdf(String storageKey, Instant generatedAt) {
        this.pdfStorageKey = Guard.requiredText(storageKey, 512, "linesheet.pdf.storage-key");
        this.pdfGeneratedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    }

    public boolean hasPdf() {
        return pdfStorageKey != null;
    }

    private static String requireReference(String referenceNumber) {
        if (referenceNumber == null || referenceNumber.length() > 24 || !REFERENCE.matcher(referenceNumber).matches()) {
            throw new InvalidValueException("linesheet.reference-number", "Invalid reference number: " + referenceNumber);
        }
        return referenceNumber;
    }

    // ------------------------------------------------------------------ odtworzenie z magazynu

    public static Builder restore() {
        return new Builder();
    }

    /** Builder do odtwarzania stanu przez adapter persystencji (zbyt wiele pól na czytelny konstruktor). */
    public static final class Builder {
        private LineSheetId id;
        private UUID publicId;
        private String referenceNumber;
        private CatalogLanguage language;
        private Currency currency;
        private Recipient recipient;
        private String message;
        private Instant privacyAcceptedAt;
        private UUID salesRepresentativeId;
        private UUID visitorId;
        private UUID buyerAccountId;
        private List<LineSheetItem> items = List.of();
        private Instant createdAt;
        private String pdfStorageKey;
        private Instant pdfGeneratedAt;
        private long version;

        private Builder() {
        }

        public Builder id(LineSheetId id) { this.id = id; return this; }
        public Builder publicId(UUID publicId) { this.publicId = publicId; return this; }
        public Builder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public Builder language(CatalogLanguage language) { this.language = language; return this; }
        public Builder currency(Currency currency) { this.currency = currency; return this; }
        public Builder recipient(Recipient recipient) { this.recipient = recipient; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder privacyAcceptedAt(Instant privacyAcceptedAt) { this.privacyAcceptedAt = privacyAcceptedAt; return this; }
        public Builder salesRepresentativeId(UUID salesRepresentativeId) { this.salesRepresentativeId = salesRepresentativeId; return this; }
        public Builder visitorId(UUID visitorId) { this.visitorId = visitorId; return this; }
        public Builder buyerAccountId(UUID buyerAccountId) { this.buyerAccountId = buyerAccountId; return this; }
        public Builder items(List<LineSheetItem> items) { this.items = items; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder pdf(String storageKey, Instant generatedAt) { this.pdfStorageKey = storageKey; this.pdfGeneratedAt = generatedAt; return this; }
        public Builder version(long version) { this.version = version; return this; }

        public LineSheet build() {
            return new LineSheet(this);
        }
    }

    // ------------------------------------------------------------------ odczyt

    public LineSheetId id() { return id; }
    public UUID publicId() { return publicId; }
    public String referenceNumber() { return referenceNumber; }
    public CatalogLanguage language() { return language; }
    public Currency currency() { return currency; }
    public Recipient recipient() { return recipient; }
    public String message() { return message; }
    public Instant privacyAcceptedAt() { return privacyAcceptedAt; }
    public UUID salesRepresentativeId() { return salesRepresentativeId; }
    public UUID visitorId() { return visitorId; }
    /** Właściciel zestawienia; {@code null} dla zestawień sprzed kont kupców lub po usunięciu konta. */
    public UUID buyerAccountId() { return buyerAccountId; }
    public boolean isOwnedBy(UUID buyer) { return buyerAccountId != null && buyerAccountId.equals(buyer); }
    public List<LineSheetItem> items() { return items; }
    public Instant createdAt() { return createdAt; }
    public String pdfStorageKey() { return pdfStorageKey; }
    public Instant pdfGeneratedAt() { return pdfGeneratedAt; }
    public long version() { return version; }
}
