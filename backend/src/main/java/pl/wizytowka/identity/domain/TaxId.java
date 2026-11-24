package pl.wizytowka.identity.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Numer identyfikacji podatkowej firmy kupca wraz z krajem rejestracji.
 *
 * <p>Wartość jest normalizowana: wielkie litery, bez spacji, kresek i kropek, bez prefiksu kraju
 * (np. {@code PL 123-456-32-18} → {@code 1234563218}). Dla Polski sprawdzana jest suma kontrolna NIP,
 * dla Czech format DIČ (8–10 cyfr); dla pozostałych krajów — ogólny format numeru VAT (2–14 znaków A–Z0–9).
 * Weryfikację w rejestrach (VIES, GUS) wykonuje marka przy akceptacji konta.</p>
 */
public record TaxId(String countryCode, String number) {

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private static final Pattern GENERIC = Pattern.compile("^[A-Z0-9]{2,14}$");
    private static final Pattern CZ = Pattern.compile("^\\d{8,10}$");
    private static final Pattern PL = Pattern.compile("^\\d{10}$");
    private static final int[] NIP_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};

    public TaxId {
        countryCode = normalizeCountry(countryCode);
        number = normalizeNumber(countryCode, number);
    }

    public static TaxId of(String countryCode, String number) {
        return new TaxId(countryCode, number);
    }

    private static String normalizeCountry(String countryCode) {
        String code = countryCode == null ? "" : countryCode.strip().toUpperCase(Locale.ROOT);
        if (!COUNTRIES.contains(code)) {
            throw new InvalidValueException("identity.buyer.country", "Unknown country code");
        }
        return code;
    }

    private static String normalizeNumber(String countryCode, String raw) {
        Objects.requireNonNull(countryCode);
        String value = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
        // prefiks kraju jak w numerze VAT UE (PL1234563218); Grecja używa w VAT prefiksu EL
        String vatPrefix = "GR".equals(countryCode) ? "EL" : countryCode;
        if (value.startsWith(vatPrefix) && value.length() > vatPrefix.length()) {
            value = value.substring(vatPrefix.length());
        }
        boolean valid = switch (countryCode) {
            case "PL" -> PL.matcher(value).matches() && hasValidNipChecksum(value);
            case "CZ" -> CZ.matcher(value).matches();
            default -> GENERIC.matcher(value).matches();
        };
        if (!valid) {
            throw new InvalidValueException("identity.buyer.tax-id", "Invalid tax identification number");
        }
        return value;
    }

    private static boolean hasValidNipChecksum(String digits) {
        int sum = 0;
        for (int i = 0; i < NIP_WEIGHTS.length; i++) {
            sum += NIP_WEIGHTS[i] * (digits.charAt(i) - '0');
        }
        int checksum = sum % 11;
        return checksum != 10 && checksum == digits.charAt(9) - '0';
    }

    /** Postać do wyświetlenia, np. {@code PL 1234563218}. */
    public String display() {
        return countryCode + " " + number;
    }
}
