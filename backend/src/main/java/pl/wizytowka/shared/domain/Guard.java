package pl.wizytowka.shared.domain;

import java.util.regex.Pattern;

/** Walidacje wartości wspólne dla modeli domenowych. Rzucają {@link InvalidValueException}. */
public final class Guard {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");

    private Guard() {
    }

    public static String requiredText(String value, int maxLength, String code) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(code, "Value is required");
        }
        return maxLength(value.strip(), maxLength, code);
    }

    public static String optionalText(String value, int maxLength, String code) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return maxLength(value.strip(), maxLength, code);
    }

    public static String slug(String value, int maxLength, String code) {
        String slug = requiredText(value, maxLength, code);
        if (!SLUG.matcher(slug).matches()) {
            throw new InvalidValueException(code, "Slug may contain lowercase letters, digits and single hyphens");
        }
        return slug;
    }

    public static String email(String value, String code) {
        String email = requiredText(value, 254, code).toLowerCase(java.util.Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new InvalidValueException(code, "Invalid e-mail address");
        }
        return email;
    }

    private static String maxLength(String value, int maxLength, String code) {
        if (value.length() > maxLength) {
            throw new InvalidValueException(code, "Value exceeds " + maxLength + " characters");
        }
        return value;
    }
}
