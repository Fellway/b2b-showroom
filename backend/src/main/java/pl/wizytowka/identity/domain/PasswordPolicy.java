package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

/**
 * Polityka haseł zgodna z NIST SP 800-63B / OWASP ASVS: liczy się długość, nie reguły złożoności.
 * Górny limit chroni przed DoS kosztownym hashowaniem bardzo długich wartości.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static String requireAcceptable(String rawPassword) {
        int length = rawPassword == null ? 0 : rawPassword.codePointCount(0, rawPassword.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new InvalidValueException("identity.password.policy",
                    "Password must have between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
        if (rawPassword.isBlank()) {
            throw new InvalidValueException("identity.password.policy", "Password must not be blank");
        }
        return rawPassword;
    }
}
