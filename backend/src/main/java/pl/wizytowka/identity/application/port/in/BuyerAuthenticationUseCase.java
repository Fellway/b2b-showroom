package pl.wizytowka.identity.application.port.in;

/** Sesja kupca: ta sama mechanika co sesja panelu (D7) — krótki JWT z rolą BUYER + rotowany refresh token. */
public interface BuyerAuthenticationUseCase {

    /**
     * @throws AuthenticationFailedException nieznany e-mail, złe hasło lub konto zablokowane (bez rozróżnienia)
     * @throws AccountNotActiveException     hasło poprawne, ale konto nie jest aktywne
     */
    BuyerSession login(String email, String password);

    /** @throws AuthenticationFailedException token nieznany, wygasły, zużyty (kradzież) lub konto nieaktywne */
    BuyerSession refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
