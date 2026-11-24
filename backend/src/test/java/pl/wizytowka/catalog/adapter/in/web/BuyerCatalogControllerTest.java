package pl.wizytowka.catalog.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.catalog.application.port.in.BuyerPricingQuery;
import pl.wizytowka.catalog.application.view.PriceView;
import pl.wizytowka.catalog.application.view.ProductPricingView;
import pl.wizytowka.catalog.domain.ProductId;
import pl.wizytowka.shared.adapter.in.web.ApiExceptionHandler;
import pl.wizytowka.shared.adapter.in.web.WebConfig;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dostęp tylko dla roli BUYER sprawdza SecurityIntegrationTest — tu filtry są wyłączone. */
@WebMvcTest(BuyerCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, ApiExceptionHandler.class})
class BuyerCatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BuyerPricingQuery pricing;

    @Test
    void returnsPricesAndEansWithoutSharedCaching() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        given(pricing.pricing(List.of(ProductId.of(productId)))).willReturn(List.of(new ProductPricingView(productId,
                new PriceView(new BigDecimal("149.00"), new BigDecimal("349.99"), "PLN", new BigDecimal("2.35")),
                List.of(new ProductPricingView.VariantCode(variantId, "LS001-NAV-M", "5901234123457")))));

        mvc.perform(get("/api/v1/buyer/catalog/pricing").param("product", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].price.wholesale").value(149.00))
                .andExpect(jsonPath("$[0].price.retail").value(349.99))
                .andExpect(jsonPath("$[0].variants[0].variantId").value(variantId.toString()))
                .andExpect(jsonPath("$[0].variants[0].ean").value("5901234123457"));
    }

    @Test
    void rejectsMissingAndTooManyProducts() throws Exception {
        mvc.perform(get("/api/v1/buyer/catalog/pricing")).andExpect(status().isBadRequest());

        given(pricing.pricing(any())).willThrow(new InvalidValueException("catalog.pricing.too-many", "Too many"));
        mvc.perform(get("/api/v1/buyer/catalog/pricing").param("product", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.pricing.too-many"));
    }
}
