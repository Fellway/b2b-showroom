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
 * Zestawienie ofertowe (line sheet) wygenerowane przez klienta B2B. Treść jest niezmienna od chwili
 * utworzenia; zmienia się wyłącznie stan techniczny (plik PDF, wysyłka e-mail).
 *
 * <p>Zgoda na przetwarzanie danych (RODO) jest wymagana, gdy klient podał jakiekolwiek dane kontaktowe
 * lub zlecił wysyłkę e-mailem; wysyłka wymaga adresu e-mail.</p>
 */
public final class LineSheet {

    public static final int MAX_ITEMS = 200;
    private static final int MAX_ERROR_LENGTH = 500;
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
    private final List<LineSheetItem> items;
    private final Instant createdAt;
    private String pdfStorageKey;
    private Instant pdfGeneratedAt;
    private EmailDeliveryStatus emailStatus;
    private int emailAttempts;
    private Instant emailSentAt;
    private String emailLastError;
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
        this.items = List.copyOf(b.items);
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.pdfStorageKey = b.pdfStorageKey;
        this.pdfGeneratedAt = b.pdfGeneratedAt;
        this.emailStatus = Objects.requireNonNull(b.emailStatus, "emailStatus");
        this.emailAttempts = b.emailAttempts;
        this.emailSentAt = b.emailSentAt;
        this.emailLastError = b.emailLastError;
        this.version = b.version;
    }

    /**
     * Tworzy zestawienie z wybranych wariantów (w kolejności prezentacji).
     * @param referenceNumber numer nadany przez generator, np. LS-2026-000042
     * @param sendEmail       czy klient zlecił wysyłkę — weryfikowane są wtedy e-mail i zgoda; samą wysyłkę
     *                        zleca {@link #requestEmailDelivery()} po dołączeniu PDF
     */
    public static LineSheet create(String referenceNumber, CatalogLanguage language, Recipient recipient, String message,
                                   boolean privacyAccepted, boolean sendEmail, UUID salesRepresentativeId,
                                   UUID visitorId, List<SelectedVariant> selections, Instant now) {
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
        if (sendEmail && !effectiveRecipient.hasEmail()) {
            throw new InvalidValueException("linesheet.email.required", "E-mail address is required to send the line sheet");
        }
        String trimmedMessage = Guard.optionalText(message, 2000, "linesheet.message");
        if ((effectiveRecipient.hasAnyData() || sendEmail) && !privacyAccepted) {
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
        b.items = items;
        b.createdAt = now;
        b.emailStatus = EmailDeliveryStatus.NOT_REQUESTED;
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

    // ------------------------------------------------------------------ e-mail

    /** Zleca (ponowną) wysyłkę: NOT_REQUESTED / FAILED / SENT → PENDING. */
    public void requestEmailDelivery() {
        if (!recipient.hasEmail()) {
            throw new BusinessRuleViolationException("linesheet.email.no-recipient", "Line sheet has no recipient e-mail");
        }
        if (privacyAcceptedAt == null) {
            throw new BusinessRuleViolationException("linesheet.privacy.consent-required", "Recipient did not accept the privacy notice");
        }
        if (!hasPdf()) {
            throw new BusinessRuleViolationException("linesheet.pdf.missing", "PDF must be generated before sending");
        }
        if (emailStatus == EmailDeliveryStatus.PENDING) {
            throw new BusinessRuleViolationException("linesheet.email.already-pending", "E-mail delivery is already in progress");
        }
        emailStatus = EmailDeliveryStatus.PENDING;
    }

    public void markEmailSent(Instant sentAt) {
        requirePending();
        emailAttempts++;
        emailStatus = EmailDeliveryStatus.SENT;
        emailSentAt = Objects.requireNonNull(sentAt, "sentAt");
        emailLastError = null;
    }

    public void markEmailFailed(String error) {
        requirePending();
        emailAttempts++;
        emailStatus = EmailDeliveryStatus.FAILED;
        String safeError = error == null || error.isBlank() ? "Unknown error" : error.strip();
        emailLastError = safeError.substring(0, Math.min(safeError.length(), MAX_ERROR_LENGTH));
    }

    private void requirePending() {
        if (emailStatus != EmailDeliveryStatus.PENDING) {
            throw new BusinessRuleViolationException("linesheet.email.not-pending",
                    "No e-mail delivery in progress, status: " + emailStatus);
        }
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
        private List<LineSheetItem> items = List.of();
        private Instant createdAt;
        private String pdfStorageKey;
        private Instant pdfGeneratedAt;
        private EmailDeliveryStatus emailStatus = EmailDeliveryStatus.NOT_REQUESTED;
        private int emailAttempts;
        private Instant emailSentAt;
        private String emailLastError;
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
        public Builder items(List<LineSheetItem> items) { this.items = items; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder pdf(String storageKey, Instant generatedAt) { this.pdfStorageKey = storageKey; this.pdfGeneratedAt = generatedAt; return this; }
        public Builder emailDelivery(EmailDeliveryStatus status, int attempts, Instant sentAt, String lastError) {
            this.emailStatus = status;
            this.emailAttempts = attempts;
            this.emailSentAt = sentAt;
            this.emailLastError = lastError;
            return this;
        }
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
    public List<LineSheetItem> items() { return items; }
    public Instant createdAt() { return createdAt; }
    public String pdfStorageKey() { return pdfStorageKey; }
    public Instant pdfGeneratedAt() { return pdfGeneratedAt; }
    public EmailDeliveryStatus emailStatus() { return emailStatus; }
    public int emailAttempts() { return emailAttempts; }
    public Instant emailSentAt() { return emailSentAt; }
    public String emailLastError() { return emailLastError; }
    public long version() { return version; }
}
