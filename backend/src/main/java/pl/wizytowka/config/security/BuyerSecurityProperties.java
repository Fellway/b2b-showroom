package pl.wizytowka.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Konta kupców (D23). Blokada po nieudanych logowaniach jest wspólna z panelem ({@link LockoutProperties}),
 * a access token i klucz podpisu — z {@link JwtProperties}.
 *
 * @param refreshTokenTtl      długość sesji kupca
 * @param emailVerificationTtl ważność linku potwierdzającego adres e-mail
 * @param passwordResetTtl     ważność linku do ustawienia nowego hasła
 */
@ConfigurationProperties("showroom.security.buyer")
public record BuyerSecurityProperties(@DefaultValue("30d") Duration refreshTokenTtl,
                                      @DefaultValue("48h") Duration emailVerificationTtl,
                                      @DefaultValue("1h") Duration passwordResetTtl) {
}
