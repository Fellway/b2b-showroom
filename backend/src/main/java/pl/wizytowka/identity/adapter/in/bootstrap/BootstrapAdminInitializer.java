package pl.wizytowka.identity.adapter.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.wizytowka.config.security.BootstrapAdminProperties;
import pl.wizytowka.identity.application.port.in.BootstrapAdminUseCase;

/** Tworzy pierwsze konto ADMIN przy pustej bazie (dane z SHOWROOM_ADMIN_EMAIL / SHOWROOM_ADMIN_PASSWORD). */
@Component
class BootstrapAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final BootstrapAdminUseCase bootstrapAdmin;
    private final BootstrapAdminProperties properties;

    BootstrapAdminInitializer(BootstrapAdminUseCase bootstrapAdmin, BootstrapAdminProperties properties) {
        this.bootstrapAdmin = bootstrapAdmin;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void createFirstAdmin() {
        if (!properties.isConfigured()) {
            return;
        }
        if (bootstrapAdmin.createFirstAdminIfNoneExists(properties.email(), properties.password())) {
            log.info("Created initial ADMIN account {}", properties.email());
        }
    }
}
