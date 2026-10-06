package pl.wizytowka.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param secret              klucz HMAC (HS256) zakodowany base64, min. 256 bitów — wyłącznie ze zmiennej środowiskowej
 * @param refreshCookieSecure flaga Secure ciasteczka z refresh tokenem; wyłączać tylko lokalnie bez HTTPS
 */
@ConfigurationProperties("showroom.security.jwt")
public record JwtProperties(String secret,
                            @DefaultValue("b2b-showroom") String issuer,
                            @DefaultValue("15m") Duration accessTokenTtl,
                            @DefaultValue("14d") Duration refreshTokenTtl,
                            @DefaultValue("true") boolean refreshCookieSecure) {
}
