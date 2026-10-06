package pl.wizytowka.linesheet.adapter.out.pdf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BrandPropertiesTest {

    @Test
    void bindsDefaultsAndTreatsBlankValuesAsMissing() {
        var source = new MapConfigurationPropertySource(Map.of(
                "showroom.brand.legal-name", "",
                "showroom.brand.email", " ",
                "showroom.brand.address-lines[0]", "ul. Lniana 7",
                "showroom.brand.address-lines[1]", ""));

        BrandProperties brand = new Binder(source).bind("showroom.brand", BrandProperties.class).get();

        assertThat(brand.name()).isEqualTo("Brand Name");
        assertThat(brand.legalName()).isNull();
        assertThat(brand.email()).isNull();
        assertThat(brand.logo()).isNull();
        assertThat(brand.addressLines()).containsExactly("ul. Lniana 7");
        assertThat(brand.accentColor()).isEqualTo("#1F2A37");
        assertThat(brand.timeZone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
    }
}
