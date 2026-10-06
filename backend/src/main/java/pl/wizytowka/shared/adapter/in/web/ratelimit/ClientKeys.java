package pl.wizytowka.shared.adapter.in.web.ratelimit;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Klucz klienta dla limitów: adres IP rozwiązany przez kontener (za reverse proxy — przez
 * {@code server.forward-headers-strategy=native}, który ufa nagłówkom wyłącznie od zaufanych proxy).
 * Nagłówków {@code X-Forwarded-*} nie czytamy tu nigdy — inaczej klient mógłby podać dowolny adres.
 * Adresy IPv6 sprowadzamy do prefiksu /64, bo pojedynczy abonent zwykle dysponuje całą podsiecią /64.
 */
final class ClientKeys {

    /** Tylko literały adresów — {@link InetAddress#getByName} nie może wykonać zapytania DNS. */
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");
    private static final int MAX_FALLBACK_LENGTH = 64;

    private ClientKeys() {
    }

    static String of(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        String literal = stripBracketsAndZone(remoteAddress.strip());
        if (!IP_LITERAL.matcher(literal).matches()) {
            return "other:" + truncate(literal);
        }
        try {
            InetAddress address = InetAddress.getByName(literal);
            return switch (address) {
                case Inet4Address ipv4 -> ipv4.getHostAddress();
                case Inet6Address ipv6 -> ipv6Prefix64(ipv6);
                default -> "other:" + truncate(literal);
            };
        } catch (UnknownHostException e) {
            return "other:" + truncate(literal);
        }
    }

    private static String ipv6Prefix64(Inet6Address address) {
        byte[] bytes = address.getAddress();
        HexFormat hex = HexFormat.of();
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < 8; i += 2) {
            prefix.append(hex.toHexDigits(bytes[i])).append(hex.toHexDigits(bytes[i + 1])).append(':');
        }
        return prefix.append(":/64").toString();
    }

    private static String stripBracketsAndZone(String value) {
        String result = value;
        if (result.startsWith("[") && result.endsWith("]")) {
            result = result.substring(1, result.length() - 1);
        }
        int zone = result.indexOf('%');
        return zone >= 0 ? result.substring(0, zone) : result;
    }

    private static String truncate(String value) {
        return value.length() <= MAX_FALLBACK_LENGTH ? value : value.substring(0, MAX_FALLBACK_LENGTH);
    }
}
