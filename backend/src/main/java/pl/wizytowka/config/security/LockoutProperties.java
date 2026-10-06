package pl.wizytowka.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("showroom.security.lockout")
public record LockoutProperties(@DefaultValue("5") int maxAttempts, @DefaultValue("15m") Duration duration) {
}
