package pl.wizytowka.analytics.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.wizytowka.analytics.application.port.in.AnalyticsDashboardUseCase;
import pl.wizytowka.analytics.application.port.in.ClientEvent;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.application.view.AnalyticsSummaryView;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AnalyticsEventsController.class, AnalyticsDashboardController.class})
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllersTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecordAnalyticsEventsUseCase recordEvents;

    @MockitoBean
    AnalyticsDashboardUseCase dashboard;

    @Test
    @SuppressWarnings("unchecked")
    void acceptsEventBatch() throws Exception {
        mvc.perform(post("/api/v1/analytics/events").header("Accept-Language", "uk-UA,uk;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"events":[{"type":"PRODUCT_VIEW","path":"/uk/katalog/koszula","properties":{"source":"grid"}},
                                           {"type":"PAGE_VIEW","language":"en","path":"/en"}]}"""))
                .andExpect(status().isAccepted());

        ArgumentCaptor<List<ClientEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(recordEvents).recordClientEvents(events.capture());
        assertThat(events.getValue()).extracting(ClientEvent::type, ClientEvent::language).containsExactly(
                org.assertj.core.groups.Tuple.tuple(AnalyticsEventType.PRODUCT_VIEW, CatalogLanguage.UK),
                org.assertj.core.groups.Tuple.tuple(AnalyticsEventType.PAGE_VIEW, CatalogLanguage.EN));
    }

    @Test
    void rejectsEmptyAndUnknownEvents() throws Exception {
        mvc.perform(post("/api/v1/analytics/events").contentType(MediaType.APPLICATION_JSON).content("{\"events\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/analytics/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"type\":\"HACKED\"}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recordEvents);
    }

    @Test
    void parsesDateRangeAsWholeUtcDays() throws Exception {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        given(dashboard.summary(from, to, CatalogLanguage.PL)).willReturn(new AnalyticsSummaryView(from, to,
                Map.of(AnalyticsEventType.PAGE_VIEW, 10L), 4, 2, List.of(), List.of(), List.of()));

        mvc.perform(get("/api/v1/admin/analytics/summary?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniqueVisitors").value(4))
                .andExpect(jsonPath("$.eventTotals.PAGE_VIEW").value(10));

        mvc.perform(get("/api/v1/admin/analytics/summary?from=yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("analytics.range.format"));
        verify(dashboard).summary(any(), any(), any());
    }
}
