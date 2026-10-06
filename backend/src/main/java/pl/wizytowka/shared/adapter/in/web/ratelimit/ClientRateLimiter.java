package pl.wizytowka.shared.adapter.in.web.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitProperties.Limit;
import pl.wizytowka.shared.adapter.in.web.ratelimit.RateLimitProperties.Rule;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Limiter typu token bucket (Bucket4j) z kubełkami per (reguła, klient) trzymanymi w ograniczonym cache Caffeine.
 * <p>
 * Stan jest lokalny dla instancji aplikacji — wystarcza przy jednej instancji. Wdrożenie wieloinstancyjne
 * wymaga współdzielonego magazynu kubełków (Bucket4j ma integracje JCache i Redis), inaczej faktyczny
 * limit mnoży się przez liczbę instancji.
 */
final class ClientRateLimiter {

    private final List<CompiledRule> rules;
    private final Cache<BucketKey, Bucket> buckets;
    private final TimeMeter timeMeter;

    ClientRateLimiter(RateLimitProperties properties, TimeMeter timeMeter) {
        this.timeMeter = Objects.requireNonNull(timeMeter, "timeMeter");
        this.rules = compile(properties.rules());
        Duration longestPeriod = rules.stream()
                .flatMap(rule -> rule.limits().stream())
                .map(Limit::period)
                .max(Duration::compareTo)
                .orElse(Duration.ofMinutes(1));
        // Kubełek nieużywany dłużej niż najdłuższy okres jest pełny — można go bezpiecznie zapomnieć.
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxTrackedClients())
                .expireAfterAccess(longestPeriod.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * @param path ścieżka w obrębie aplikacji, już zdekodowana (tak jak widzi ją routing Spring MVC)
     */
    RateLimitDecision tryAcquire(String method, String path, String clientKey) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        long remaining = Long.MAX_VALUE;
        boolean matched = false;
        for (CompiledRule rule : rules) {
            if (!rule.method().equals(normalizedMethod) || !rule.pattern().matches(pathContainer)) {
                continue;
            }
            matched = true;
            Bucket bucket = buckets.get(new BucketKey(rule.name(), clientKey), key -> newBucket(rule));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + 999_999_999L);
                return new RateLimitDecision.Rejected(rule.name(), Math.max(1, seconds));
            }
            remaining = Math.min(remaining, probe.getRemainingTokens());
        }
        return matched ? new RateLimitDecision.Allowed(remaining) : new RateLimitDecision.NotLimited();
    }

    /** Do testów i metryk: przybliżona liczba śledzonych kubełków. */
    long trackedBuckets() {
        buckets.cleanUp();
        return buckets.estimatedSize();
    }

    private Bucket newBucket(CompiledRule rule) {
        LocalBucketBuilder builder = Bucket.builder().withCustomTimePrecision(timeMeter);
        for (Limit limit : rule.limits()) {
            builder.addLimit(Bandwidth.builder()
                    .capacity(limit.capacity())
                    .refillGreedy(limit.capacity(), limit.period())
                    .build());
        }
        return builder.build();
    }

    private static List<CompiledRule> compile(List<Rule> rules) {
        Set<String> names = new HashSet<>();
        PathPatternParser parser = PathPatternParser.defaultInstance;
        return rules.stream()
                .map(rule -> {
                    if (!names.add(rule.name())) {
                        throw new IllegalArgumentException("Duplicate rate limit rule name: " + rule.name());
                    }
                    return new CompiledRule(rule.name(), rule.method().toUpperCase(Locale.ROOT),
                            parser.parse(rule.path()), List.copyOf(rule.limits()));
                })
                .toList();
    }

    private record CompiledRule(String name, String method, PathPattern pattern, List<Limit> limits) {
    }

    private record BucketKey(String rule, String client) {
    }
}
