package pl.wizytowka.catalog.application.view;

import pl.wizytowka.shared.domain.Pricing;

import java.math.BigDecimal;

/** @param markup mnożnik ceny detalicznej względem hurtowej */
public record PriceView(BigDecimal wholesale, BigDecimal retail, String currency, BigDecimal markup) {

    public static PriceView of(Pricing pricing) {
        return new PriceView(pricing.wholesalePrice(), pricing.retailPrice(),
                pricing.currency().getCurrencyCode(), pricing.markupMultiplier());
    }
}
