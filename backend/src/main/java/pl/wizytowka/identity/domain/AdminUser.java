package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.Guard;

import java.time.Instant;
import java.util.Objects;

/** Konto panelu administracyjnego. Hasło istnieje wyłącznie jako hash. */
public final class AdminUser {

    private final AdminUserId id;
    private final String email;
    private String passwordHash;
    private String displayName;
    private AdminRole role;
    private boolean enabled;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private final long version;

    private AdminUser(AdminUserId id, String email, String passwordHash, String displayName, AdminRole role,
                      boolean enabled, int failedLoginAttempts, Instant lockedUntil, Instant lastLoginAt, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Guard.email(email, "identity.email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.displayName = Guard.requiredText(displayName, 120, "identity.display-name");
        this.role = Objects.requireNonNull(role, "role");
        this.enabled = enabled;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.version = version;
    }

    public static AdminUser create(String email, String displayName, AdminRole role, String passwordHash) {
        return new AdminUser(AdminUserId.newId(), email, passwordHash, displayName, role, true, 0, null, null, 0);
    }

    public static AdminUser restore(AdminUserId id, String email, String passwordHash, String displayName, AdminRole role,
                                    boolean enabled, int failedLoginAttempts, Instant lockedUntil, Instant lastLoginAt,
                                    long version) {
        return new AdminUser(id, email, passwordHash, displayName, role, enabled, failedLoginAttempts, lockedUntil,
                lastLoginAt, version);
    }

    public static String normalizeEmail(String email) {
        return Guard.email(email, "identity.email");
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Czy konto może się teraz zalogować (niezależnie od poprawności hasła). */
    public boolean canLogIn(Instant now) {
        return enabled && !isLocked(now);
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

    public void update(String displayName, AdminRole role) {
        this.displayName = Guard.requiredText(displayName, 120, "identity.display-name");
        this.role = Objects.requireNonNull(role, "role");
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean isEnabledAdmin() {
        return enabled && role == AdminRole.ADMIN;
    }

    public AdminUserId id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public AdminRole role() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public long version() {
        return version;
    }

    @Override
    public String toString() {
        return "AdminUser[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}
