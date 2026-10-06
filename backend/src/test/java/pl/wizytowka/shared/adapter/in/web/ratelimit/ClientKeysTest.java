package pl.wizytowka.shared.adapter.in.web.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClientKeysTest {

    @Test
    void keepsIpv4AddressAsIs() {
        assertThat(ClientKeys.of("203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @Test
    void groupsIpv6AddressesByPrefix64() {
        String first = ClientKeys.of("2001:db8:1:2:aaaa:bbbb:cccc:dddd");
        String sameSubnet = ClientKeys.of("[2001:0db8:0001:0002::1%eth0]");
        String otherSubnet = ClientKeys.of("2001:db8:1:3::1");

        assertThat(first).isEqualTo("2001:0db8:0001:0002::/64").isEqualTo(sameSubnet);
        assertThat(otherSubnet).isNotEqualTo(first);
    }

    @Test
    void treatsIpv4MappedIpv6AsIpv4() {
        assertThat(ClientKeys.of("::ffff:203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "localhost", "not an address"})
    void neverResolvesHostNames(String value) {
        assertThat(ClientKeys.of(value)).isEqualTo("other:" + value);
    }

    @Test
    void handlesMissingAndOversizedValues() {
        assertThat(ClientKeys.of(null)).isEqualTo("unknown");
        assertThat(ClientKeys.of(" ")).isEqualTo("unknown");
        assertThat(ClientKeys.of("x".repeat(500))).hasSize("other:".length() + 64);
    }
}
