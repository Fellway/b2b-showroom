package pl.wizytowka.shared.adapter.in.web.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Limity zapytań dla endpointów podatnych na nadużycia (generowanie PDF, analityka, logowanie).
 *
 * @param enabled           wyłącznik globalny (np. w testach obciążeniowych za własnym limiterem)
 * @param maxTrackedClients górna granica liczby śledzonych kubełków — pamięć pozostaje ograniczona
 *                          nawet przy zalewaniu żądaniami z wielu adresów
 * @param rules             reguły; każde żądanie zużywa token z każdej pasującej reguły
 */
@Validated
@ConfigurationProperties("showroom.rate-limit")
public record RateLimitProperties(@DefaultValue("true") boolean enabled,
                                  @DefaultValue("100000") @Positive int maxTrackedClients,
                                  @DefaultValue List<@Valid @NotNull Rule> rules) {

    /**
     * @param name   unikalna nazwa reguły (część klucza kubełka)
     * @param method metoda HTTP, np. {@code POST}
     * @param path   wzorzec ścieżki w składni Spring {@code PathPattern}, np. {@code /api/v1/line-sheets}
     * @param limits limity stosowane łącznie (np. 5/min i 30/h)
     */
    public record Rule(@NotBlank String name,
                       @NotBlank @Pattern(regexp = "^[A-Za-z]+$") String method,
                       @NotBlank @Pattern(regexp = "^/.*") String path,
                       @NotEmpty List<@Valid @NotNull Limit> limits) {
    }

    /** {@code capacity} żądań na {@code period}, z ciągłym (zachłannym) odnawianiem tokenów. */
    public record Limit(@Positive long capacity, @NotNull Duration period) {
    }
}
