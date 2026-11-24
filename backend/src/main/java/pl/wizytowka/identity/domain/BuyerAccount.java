package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Konto kupca hurtowego (D23). Ceny, kody EAN i zestawienia PDF są dostępne wyłącznie dla konta {@link BuyerStatus#ACTIVE}.
 * Konto powstaje z rejestracji (z wymaganymi zgodami), po potwierdzeniu adresu e-mail czeka na decyzję marki.
 * Hasło istnieje wyłącznie jako hash.
 */
public final class BuyerAccount {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()./-]{6,32}$");
    private static final Set<BuyerStatus> APPROVABLE = EnumSet.of(BuyerStatus.PENDING_APPROVAL, BuyerStatus.REJECTED);

    private final BuyerAccountId id;
    private final String email;
    private String passwordHash;
    private final String companyName;
    private final TaxId taxId;
    private String contactName;
    private String phone;
    private CatalogLanguage language;
    private BuyerStatus status;
    private final Instant termsAcceptedAt;
    private final Instant privacyAcceptedAt;
    private Instant emailVerifiedAt;
    private Instant reviewedAt;
    private AdminUserId reviewedBy;
    private String reviewNote;
    private final Instant createdAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private final long version;

    private BuyerAccount(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.email = Guard.email(b.email, "identity.email");
        this.passwordHash = Objects.requireNonNull(b.passwordHash, "passwordHash");
        this.companyName = Guard.requiredText(b.companyName, 200, "identity.buyer.company-name");
        this.taxId = Objects.requireNonNull(b.taxId, "taxId");
        this.contactName = Guard.requiredText(b.contactName, 120, "identity.buyer.contact-name");
        this.phone = phone(b.phone);
        this.language = Objects.requireNonNull(b.language, "language");
        this.status = Objects.requireNonNull(b.status, "status");
        this.termsAcceptedAt = Objects.requireNonNull(b.termsAcceptedAt, "termsAcceptedAt");
        this.privacyAcceptedAt = Objects.requireNonNull(b.privacyAcceptedAt, "privacyAcceptedAt");
        this.emailVerifiedAt = b.emailVerifiedAt;
        this.reviewedAt = b.reviewedAt;
        this.reviewedBy = b.reviewedBy;
        this.reviewNote = Guard.optionalText(b.reviewNote, 500, "identity.buyer.review-note");
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.failedLoginAttempts = b.failedLoginAttempts;
        this.lockedUntil = b.lockedUntil;
        this.lastLoginAt = b.lastLoginAt;
        this.version = b.version;
    }

    /**
     * Rejestracja nowego kupca — konto czeka na potwierdzenie adresu e-mail.
     * @throws InvalidValueException gdy brakuje wymaganych zgód lub danych
     */
    public static BuyerAccount register(String email, String passwordHash, String companyName, TaxId taxId,
                                        String contactName, String phone, CatalogLanguage language,
                                        boolean termsAccepted, boolean privacyAccepted, Instant now) {
        if (!termsAccepted || !privacyAccepted) {
            throw new InvalidValueException("identity.buyer.consent-required",
                    "Accepting the terms and the privacy policy is required");
        }
        Builder b = new Builder();
        b.id = BuyerAccountId.newId();
        b.email = email;
        b.passwordHash = passwordHash;
        b.companyName = companyName;
        b.taxId = taxId;
        b.contactName = contactName;
        b.phone = phone;
        b.language = language;
        b.status = BuyerStatus.PENDING_EMAIL;
        b.termsAcceptedAt = Objects.requireNonNull(now, "now");
        b.privacyAcceptedAt = now;
        b.createdAt = now;
        return new BuyerAccount(b);
    }

    public static String normalizeEmail(String email) {
        return Guard.email(email, "identity.email");
    }

    // ------------------------------------------------------------------ cykl życia

    /** @return {@code true}, gdy adres został potwierdzony teraz (ponowne potwierdzenie niczego nie zmienia) */
    public boolean verifyEmail(Instant now) {
        if (status != BuyerStatus.PENDING_EMAIL) {
            return false;
        }
        emailVerifiedAt = Objects.requireNonNull(now);
        status = BuyerStatus.PENDING_APPROVAL;
        return true;
    }

    public void approve(AdminUserId reviewer, Instant now) {
        if (status == BuyerStatus.PENDING_EMAIL) {
            throw new BusinessRuleViolationException("identity.buyer.email-not-verified",
                    "The buyer has not confirmed the e-mail address yet");
        }
        requireStatus(APPROVABLE);
        review(BuyerStatus.ACTIVE, reviewer, null, now);
    }

    public void reject(AdminUserId reviewer, String note, Instant now) {
        requireStatus(EnumSet.of(BuyerStatus.PENDING_APPROVAL));
        review(BuyerStatus.REJECTED, reviewer, note, now);
    }

    public void suspend(AdminUserId reviewer, String note, Instant now) {
        requireStatus(EnumSet.of(BuyerStatus.ACTIVE));
        review(BuyerStatus.SUSPENDED, reviewer, note, now);
    }

    public void reactivate(AdminUserId reviewer, Instant now) {
        requireStatus(EnumSet.of(BuyerStatus.SUSPENDED));
        review(BuyerStatus.ACTIVE, reviewer, null, now);
    }

    private void review(BuyerStatus target, AdminUserId reviewer, String note, Instant now) {
        this.status = target;
        this.reviewedBy = Objects.requireNonNull(reviewer, "reviewer");
        this.reviewedAt = Objects.requireNonNull(now, "now");
        this.reviewNote = Guard.optionalText(note, 500, "identity.buyer.review-note");
    }

    private void requireStatus(Set<BuyerStatus> allowed) {
        if (!allowed.contains(status)) {
            throw new BusinessRuleViolationException("identity.buyer.invalid-transition",
                    "Operation is not allowed for an account in status " + status);
        }
    }

    public boolean isActive() {
        return status == BuyerStatus.ACTIVE;
    }

    // ------------------------------------------------------------------ logowanie

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(Instant now, LockoutPolicy policy) {
        if (isLocked(now)) {
            return;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= policy.maxAttempts()) {
            lockedUntil = now.plus(policy.lockDuration());
            failedLoginAttempts = 0;
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = Objects.requireNonNull(now);
    }

    /** Nowe hasło zdejmuje też blokadę po nieudanych logowaniach. */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    // ------------------------------------------------------------------ profil

    /** Kupiec zmienia sam tylko dane kontaktowe i język; firmę i NIP zmienia marka (nowa weryfikacja). */
    public void updateProfile(String contactName, String phone, CatalogLanguage language) {
        this.contactName = Guard.requiredText(contactName, 120, "identity.buyer.contact-name");
        this.phone = phone(phone);
        this.language = Objects.requireNonNull(language, "language");
    }

    private static String phone(String value) {
        String phone = Guard.requiredText(value, 32, "identity.buyer.phone");
        if (!PHONE.matcher(phone).matches() || phone.chars().filter(Character::isDigit).count() < 6) {
            throw new InvalidValueException("identity.buyer.phone", "Invalid phone number");
        }
        return phone;
    }

    // ------------------------------------------------------------------ odtworzenie z magazynu

    public static Builder restore() {
        return new Builder();
    }

    /** Builder do odtwarzania stanu przez adapter persystencji. */
    public static final class Builder {
        private BuyerAccountId id;
        private String email;
        private String passwordHash;
        private String companyName;
        private TaxId taxId;
        private String contactName;
        private String phone;
        private CatalogLanguage language;
        private BuyerStatus status;
        private Instant termsAcceptedAt;
        private Instant privacyAcceptedAt;
        private Instant emailVerifiedAt;
        private Instant reviewedAt;
        private AdminUserId reviewedBy;
        private String reviewNote;
        private Instant createdAt;
        private int failedLoginAttempts;
        private Instant lockedUntil;
        private Instant lastLoginAt;
        private long version;

        private Builder() {
        }

        public Builder id(BuyerAccountId id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder taxId(TaxId taxId) { this.taxId = taxId; return this; }
        public Builder contactName(String contactName) { this.contactName = contactName; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder language(CatalogLanguage language) { this.language = language; return this; }
        public Builder status(BuyerStatus status) { this.status = status; return this; }
        public Builder termsAcceptedAt(Instant termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; return this; }
        public Builder privacyAcceptedAt(Instant privacyAcceptedAt) { this.privacyAcceptedAt = privacyAcceptedAt; return this; }
        public Builder emailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; return this; }
        public Builder reviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder reviewedBy(AdminUserId reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder reviewNote(String reviewNote) { this.reviewNote = reviewNote; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder failedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; return this; }
        public Builder lockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; return this; }
        public Builder lastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        public Builder version(long version) { this.version = version; return this; }

        public BuyerAccount build() {
            return new BuyerAccount(this);
        }
    }

    // ------------------------------------------------------------------ odczyt

    public BuyerAccountId id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String companyName() { return companyName; }
    public TaxId taxId() { return taxId; }
    public String contactName() { return contactName; }
    public String phone() { return phone; }
    public CatalogLanguage language() { return language; }
    public BuyerStatus status() { return status; }
    public Instant termsAcceptedAt() { return termsAcceptedAt; }
    public Instant privacyAcceptedAt() { return privacyAcceptedAt; }
    public Instant emailVerifiedAt() { return emailVerifiedAt; }
    public Instant reviewedAt() { return reviewedAt; }
    public AdminUserId reviewedBy() { return reviewedBy; }
    public String reviewNote() { return reviewNote; }
    public Instant createdAt() { return createdAt; }
    public int failedLoginAttempts() { return failedLoginAttempts; }
    public Instant lockedUntil() { return lockedUntil; }
    public Instant lastLoginAt() { return lastLoginAt; }
    public long version() { return version; }

    @Override
    public String toString() {
        return "BuyerAccount[id=" + id + ", status=" + status + "]";
    }
}
