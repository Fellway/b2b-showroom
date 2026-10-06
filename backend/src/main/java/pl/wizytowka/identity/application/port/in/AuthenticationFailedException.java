package pl.wizytowka.identity.application.port.in;

/**
 * Nieudane uwierzytelnienie (HTTP 401). Celowo nie rozróżnia przyczyn (nieznany e-mail, złe hasło,
 * konto zablokowane lub wyłączone), żeby nie ujawniać, które konta istnieją.
 */
public final class AuthenticationFailedException extends RuntimeException {

    public static final String INVALID_CREDENTIALS = "identity.invalid-credentials";
    public static final String INVALID_REFRESH_TOKEN = "identity.invalid-refresh-token";

    private final String code;

    public AuthenticationFailedException(String code) {
        super("Authentication failed");
        this.code = code;
    }

    public String code() {
        return code;
    }
}
