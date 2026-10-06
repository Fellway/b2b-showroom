package pl.wizytowka.identity.application.port.in;

public interface AuthenticateUseCase {

    /** @throws AuthenticationFailedException z kodem {@link AuthenticationFailedException#INVALID_CREDENTIALS} */
    AuthenticationResult login(String email, String password);

    /**
     * Wymienia refresh token na nową parę tokenów (rotacja). Ponowne użycie zużytego tokenu unieważnia całą sesję.
     * @throws AuthenticationFailedException z kodem {@link AuthenticationFailedException#INVALID_REFRESH_TOKEN}
     */
    AuthenticationResult refresh(String rawRefreshToken);

    /** Unieważnia sesję (rodzinę tokenów). Nieznany token jest ignorowany. */
    void logout(String rawRefreshToken);
}
