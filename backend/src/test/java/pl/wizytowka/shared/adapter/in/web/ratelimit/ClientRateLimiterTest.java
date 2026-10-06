package pl.wizytowka.shared.adapter.in.web.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitDecision.Allowed;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitDecision.NotLimited;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitDecision.Rejected;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitProperties.Limit;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitProperties.Rule;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientRateLimiterTest {

    private static final Rule LINE_SHEETS = new Rule("line-sheets", "POST", "/api/v1/line-sheets",
            List.of(new Limit(5, Duration.ofMinutes(1)), new Limit(30, Duration.ofHours(1))));
    private static final Rule ITEMS = new Rule("items", "post", "/api/v1/items/{id}/notes",
            List.of(new Limit(1, Duration.ofSeconds(10))));

    private final ManualTimeMeter clock = new ManualTimeMeter();
    private final ClientRateLimiter limiter = limiter(100, LINE_SHEETS, ITEMS);

    @Test
    void allowsUpToCapacityThenRejectsWithRetryAfter() {
        for (int remaining = 4; remaining >= 0; remaining--) {
            assertThat(acquire("1.1.1.1")).isEqualTo(new Allowed(remaining));
        }

        assertThat(acquire("1.1.1.1")).isInstanceOfSatisfying(Rejected.class, rejected -> {
            assertThat(rejected.rule()).isEqualTo("line-sheets");
            assertThat(rejected.retryAfterSeconds()).isEqualTo(12); // 1 token co 60/5 = 12 s
        });
    }

    @Test
    void refillsGraduallyOverTime() {
        exhaust("1.1.1.1", 5);

        clock.advance(Duration.ofSeconds(11));
        assertThat(acquire("1.1.1.1")).isInstanceOf(Rejected.class);
        clock.advance(Duration.ofSeconds(1));
        assertThat(acquire("1.1.1.1")).isInstanceOf(Allowed.class);
    }

    @Test
    void hourlyLimitAppliesEvenWhenMinuteLimitRefilled() {
        for (int minute = 0; minute < 6; minute++) {
            exhaust("1.1.1.1", 5);
            clock.advance(Duration.ofMinutes(1));
        }
        // limit minutowy jest znów pełny (5), ale godzinowy odzyskał przez 6 minut tylko 6 × 0,5 = 3 tokeny
        exhaust("1.1.1.1", 3);

        assertThat(acquire("1.1.1.1")).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.retryAfterSeconds()).isEqualTo(120)); // 1 token co 3600/30 s
    }

    @Test
    void clientsAndRulesHaveIndependentBuckets() {
        exhaust("1.1.1.1", 5);

        assertThat(acquire("2.2.2.2")).isEqualTo(new Allowed(4));
        assertThat(limiter.tryAcquire("POST", "/api/v1/items/7/notes", "1.1.1.1")).isEqualTo(new Allowed(0));
        assertThat(limiter.tryAcquire("POST", "/api/v1/items/8/notes", "1.1.1.1"))
                .as("wzorzec ze zmienną to jedna reguła — jeden kubełek na klienta")
                .isInstanceOf(Rejected.class);
    }

    @Test
    void ignoresOtherMethodsAndPaths() {
        assertThat(limiter.tryAcquire("GET", "/api/v1/line-sheets", "1.1.1.1")).isInstanceOf(NotLimited.class);
        assertThat(limiter.tryAcquire("POST", "/api/v1/line-sheets/x", "1.1.1.1")).isInstanceOf(NotLimited.class);
        assertThat(limiter.tryAcquire("post", "/api/v1/line-sheets", "1.1.1.1")).isInstanceOf(Allowed.class);
    }

    @Test
    void keepsMemoryBoundedUnderAddressSpraying() {
        ClientRateLimiter small = limiter(50, LINE_SHEETS);
        for (int i = 0; i < 1_000; i++) {
            small.tryAcquire("POST", "/api/v1/line-sheets", "10.0." + (i / 256) + "." + (i % 256));
        }

        assertThat(small.trackedBuckets()).isLessThanOrEqualTo(50);
    }

    @Test
    void rejectsDuplicateRuleNames() {
        assertThatThrownBy(() -> limiter(10, LINE_SHEETS, LINE_SHEETS)).isInstanceOf(IllegalArgumentException.class);
    }

    private RateLimitDecision acquire(String client) {
        return limiter.tryAcquire("POST", "/api/v1/line-sheets", client);
    }

    private void exhaust(String client, int times) {
        for (int i = 0; i < times; i++) {
            assertThat(acquire(client)).isInstanceOf(Allowed.class);
        }
    }

    private ClientRateLimiter limiter(int maxClients, Rule... rules) {
        return new ClientRateLimiter(new RateLimitProperties(true, maxClients, List.of(rules)), clock);
    }

    private static final class ManualTimeMeter implements TimeMeter {

        private long nanos = Duration.ofDays(1).toNanos();

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}
