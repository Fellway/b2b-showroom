package pl.wizytowka.identity.adapter.out.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * E-maile transakcyjne kont kupców (D23). Serwer SMTP konfiguruje się standardowo ({@code spring.mail.*}).
 *
 * @param enabled       {@code false} (domyślnie, dev) — wiadomości trafiają do logu zamiast do SMTP
 * @param from          adres nadawcy, np. {@code "Fabian's Style B2B <b2b@marka.pl>"} (domena z SPF/DKIM)
 * @param publicBaseUrl publiczny adres showroomu bez końcowego ukośnika — z niego budowane są linki w e-mailach
 */
@ConfigurationProperties("showroom.mail")
public record MailProperties(@DefaultValue("false") boolean enabled,
                             @DefaultValue("B2B Showroom <no-reply@localhost>") String from,
                             @DefaultValue("http://localhost:3000") String publicBaseUrl) {

    public MailProperties {
        publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }
}
