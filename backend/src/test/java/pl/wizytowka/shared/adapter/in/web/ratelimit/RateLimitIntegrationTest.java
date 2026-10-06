package pl.wizytowka.shared.adapter.in.web.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.wizytowka.support.TestcontainersConfiguration;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limit na prawdziwym łańcuchu filtrów (przed Spring Security), z niskim limitem testowym. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class})
@TestPropertySource(properties = {
        "showroom.security.jwt.secret=c2hvd3Jvb20tcmF0ZS1saW1pdC1zZWNyZXQtbG9uZy1lbm91Z2gtZm9yLWhzMjU2",
        "showroom.storage.local-root=target/rate-limit-storage",
        "showroom.rate-limit.rules[0].name=analytics-events",
        "showroom.rate-limit.rules[0].method=POST",
        "showroom.rate-limit.rules[0].path=/api/v1/analytics/events",
        "showroom.rate-limit.rules[0].limits[0].capacity=3",
        "showroom.rate-limit.rules[0].limits[0].period=1h"
})
class RateLimitIntegrationTest {

    private static final String EVENTS = """
            {"events":[{"type":"PAGE_VIEW","path":"/"}]}""";

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsRequestsAboveLimitWithProblemDetailAndRetryAfter() throws Exception {
        String client = "198.51.100.1";
        sendEvent(client).andExpect(status().isAccepted()).andExpect(header().string("X-RateLimit-Remaining", "2"));
        sendEvent(client).andExpect(status().isAccepted()).andExpect(header().string("X-RateLimit-Remaining", "1"));
        sendEvent(client).andExpect(status().isAccepted()).andExpect(header().string("X-RateLimit-Remaining", "0"));

        sendEvent(client)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1200"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.code").value("request.rate-limited"))
                .andExpect(jsonPath("$.instance").value("/api/v1/analytics/events"));
    }

    @Test
    void clientsAreLimitedIndependently() throws Exception {
        exhaust("198.51.100.2");

        sendEvent("198.51.100.3").andExpect(status().isAccepted());
    }

    @Test
    void spoofedForwardedForHeaderDoesNotChangeTheClient() throws Exception {
        String client = "198.51.100.4";
        exhaust(client);

        mvc.perform(withClient(post("/api/v1/analytics/events"), client)
                        .header("X-Forwarded-For", "203.0.113.99")
                        .header("Forwarded", "for=203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENTS))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void encodedPathCannotBypassTheLimit() throws Exception {
        String client = "198.51.100.5";
        exhaust(client);

        mvc.perform(withClient(post(URI.create("/api/v1/analytics/even%74s")), client)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENTS))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void otherEndpointsAreNotLimited() throws Exception {
        String client = "198.51.100.6";
        exhaust(client);

        for (int i = 0; i < 5; i++) {
            mvc.perform(withClient(get("/api/v1/catalog/categories"), client))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-RateLimit-Remaining"));
        }
    }

    private void exhaust(String client) throws Exception {
        for (int i = 0; i < 3; i++) {
            sendEvent(client).andExpect(status().isAccepted());
        }
        sendEvent(client).andExpect(status().isTooManyRequests());
    }

    private ResultActions sendEvent(String client) throws Exception {
        return mvc.perform(withClient(post("/api/v1/analytics/events"), client)
                .contentType(MediaType.APPLICATION_JSON).content(EVENTS));
    }

    private static MockHttpServletRequestBuilder withClient(MockHttpServletRequestBuilder request, String address) {
        return request.with(r -> {
            r.setRemoteAddr(address);
            return r;
        });
    }
}
