package pl.wizytowka.identity.adapter.out.mail;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import pl.wizytowka.config.security.BuyerSecurityProperties;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wysyłka e-maili transakcyjnych przez SMTP ({@code spring.mail.*}). Wysyłka odbywa się w tle: odpowiedź API nie
 * czeka na serwer poczty (czas odpowiedzi nie zdradza, czy konto istnieje), a błąd wysyłki jest tylko logowany.
 *
 * <p>Gdy {@code showroom.mail.enabled=false} (dev), treść wiadomości trafia do logu — razem z linkiem z tokenem,
 * dlatego ten tryb jest wyłącznie lokalny.</p>
 */
@Component
class TransactionalBuyerMailer implements BuyerMailer {

    private static final Logger log = LoggerFactory.getLogger(TransactionalBuyerMailer.class);

    private final MailProperties properties;
    private final BuyerSecurityProperties security;
    private final JavaMailSender sender;
    private final String brand;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    TransactionalBuyerMailer(MailProperties properties, BuyerSecurityProperties security,
                             ObjectProvider<JavaMailSender> sender,
                             @Value("${showroom.brand.name:B2B Showroom}") String brand) {
        this.properties = properties;
        this.security = security;
        this.sender = sender.getIfAvailable();
        this.brand = brand;
        if (properties.enabled() && this.sender == null) {
            throw new IllegalStateException("showroom.mail.enabled=true requires an SMTP server (spring.mail.host)");
        }
        if (!properties.enabled()) {
            log.warn("Transactional e-mails are disabled (showroom.mail.enabled=false) — messages are only logged");
        }
    }

    @Override
    public void sendEmailVerification(String to, CatalogLanguage language, String rawToken) {
        send(to, MailTemplates.render(MailTemplates.Kind.VERIFY_EMAIL, language, brand,
                link(language, "/account/verify-email?token=" + encode(rawToken)), validity(security.emailVerificationTtl())));
    }

    @Override
    public void sendAccountAlreadyExists(String to, CatalogLanguage language) {
        send(to, MailTemplates.render(MailTemplates.Kind.ACCOUNT_EXISTS, language, brand,
                link(language, "/account/login"), null));
    }

    @Override
    public void sendAccountApproved(String to, CatalogLanguage language) {
        send(to, MailTemplates.render(MailTemplates.Kind.ACCOUNT_APPROVED, language, brand,
                link(language, "/account/login"), null));
    }

    @Override
    public void sendAccountRejected(String to, CatalogLanguage language) {
        send(to, MailTemplates.render(MailTemplates.Kind.ACCOUNT_REJECTED, language, brand, link(language, ""), null));
    }

    @Override
    public void sendPasswordReset(String to, CatalogLanguage language, String rawToken) {
        send(to, MailTemplates.render(MailTemplates.Kind.PASSWORD_RESET, language, brand,
                link(language, "/account/reset-password?token=" + encode(rawToken)), validity(security.passwordResetTtl())));
    }

    @Override
    public void sendAccountDeleted(String to, CatalogLanguage language) {
        send(to, MailTemplates.render(MailTemplates.Kind.ACCOUNT_DELETED, language, brand, link(language, ""), null));
    }

    @Override
    public void notifyStaffAboutPendingBuyer(List<String> staffEmails) {
        MailTemplates.Message message = MailTemplates.staffPendingBuyer(properties.publicBaseUrl() + "/admin/buyers");
        staffEmails.forEach(to -> send(to, message));
    }

    /** Polski bez prefiksu ścieżki (rynek główny), pozostałe języki pod /en, /cs, /uk — jak w routingu frontendu. */
    private String link(CatalogLanguage language, String path) {
        String prefix = language == CatalogLanguage.DEFAULT ? "" : "/" + language.code();
        String full = prefix + path;
        return properties.publicBaseUrl() + (full.isEmpty() ? "/" : full);
    }

    private static String validity(Duration ttl) {
        return ttl.toMinutes() % 60 == 0 ? ttl.toHours() + " h" : ttl.toMinutes() + " min";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void send(String to, MailTemplates.Message message) {
        if (!properties.enabled()) {
            log.info("[e-mail disabled] to={} subject=\"{}\"\n{}", to, message.subject(), message.body());
            return;
        }
        executor.submit(() -> {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(properties.from());
                mail.setTo(to);
                mail.setSubject(message.subject());
                mail.setText(message.body());
                sender.send(mail);
            } catch (RuntimeException e) {
                // nie logujemy adresu odbiorcy ani treści (link z tokenem)
                log.warn("Sending transactional e-mail failed: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.close();
    }
}
