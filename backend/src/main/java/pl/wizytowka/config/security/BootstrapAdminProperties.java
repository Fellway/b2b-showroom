package pl.wizytowka.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dane pierwszego administratora, używane tylko gdy w bazie nie ma jeszcze żadnego konta. */
@ConfigurationProperties("showroom.security.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "BootstrapAdminProperties[email=" + email + "]";
    }
}
