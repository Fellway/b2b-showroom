package pl.wizytowka.identity.domain;

import java.time.Duration;

/** Do czego służy jednorazowy token wysłany e-mailem. */
public enum BuyerActionPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET;

    /** Najkrótszy odstęp między kolejnymi e-mailami tego rodzaju do jednego konta (ochrona skrzynki przed zalewem). */
    public static final Duration RESEND_COOLDOWN = Duration.ofMinutes(2);
}
