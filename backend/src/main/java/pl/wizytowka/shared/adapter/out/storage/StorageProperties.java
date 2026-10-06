package pl.wizytowka.shared.adapter.out.storage;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/** @param localRoot katalog bazowy magazynu plików na dysku (adapter lokalny) */
@Validated
@ConfigurationProperties("showroom.storage")
public record StorageProperties(@NotNull Path localRoot) {
}
