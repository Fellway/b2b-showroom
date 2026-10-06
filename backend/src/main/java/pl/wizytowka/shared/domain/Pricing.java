package pl.wizytowka.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Para cen B2B: hurtowa (netto) i sugerowana detaliczna (MSRP/RRP, brutto) w jednej walucie. */
public record Pricing(BigDecimal wholesalePrice, BigDecimal retailPrice, Currency currency) {

    public static final Currency PLN = Currency.getInstance("PLN");
    private static final int SCALE = 2;
    private static final BigDecimal MAX = new BigDecimal("9999999999.99");

    public Pricing {
        Objects.requireNonNull(wholesalePrice, "wholesalePrice");
        Objects.requireNonNull(retailPrice, "retailPrice");
        Objects.requireNonNull(currency, "currency");
        if (wholesalePrice.signum() <= 0 || retailPrice.signum() <= 0) {
            throw new InvalidValueException("pricing.non-positive", "Prices must be positive");
        }
        if (wholesalePrice.compareTo(MAX) > 0 || retailPrice.compareTo(MAX) > 0) {
            throw new InvalidValueException("pricing.too-large", "Price exceeds allowed maximum");
        }
        if (wholesalePrice.stripTrailingZeros().scale() > SCALE || retailPrice.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidValueException("pricing.precision", "Prices must have at most " + SCALE + " decimal places");
        }
        wholesalePrice = wholesalePrice.setScale(SCALE, RoundingMode.UNNECESSARY);
        retailPrice = retailPrice.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Pricing pln(String wholesale, String retail) {
        return new Pricing(new BigDecimal(wholesale), new BigDecimal(retail), PLN);
    }

    /** Mnożnik ceny detalicznej względem hurtowej (np. 2.35), prezentowany klientom B2B. */
    public BigDecimal markupMultiplier() {
        return retailPrice.divide(wholesalePrice, 2, RoundingMode.HALF_UP);
    }
}
