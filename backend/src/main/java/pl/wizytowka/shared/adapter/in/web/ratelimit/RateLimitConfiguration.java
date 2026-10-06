package pl.wizytowka.shared.adapter.in.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "showroom.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
class RateLimitConfiguration {

    /** Przed łańcuchem Spring Security — nadmiarowe żądania nie docierają do uwierzytelniania ani bazy. */
    static final int FILTER_ORDER = SecurityProperties.DEFAULT_FILTER_ORDER - 50;

    @Bean
    ClientRateLimiter clientRateLimiter(RateLimitProperties properties) {
        return new ClientRateLimiter(properties, TimeMeter.SYSTEM_MILLISECONDS);
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(ClientRateLimiter limiter, ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(limiter, objectMapper));
        registration.setName("rateLimitFilter");
        registration.setOrder(FILTER_ORDER);
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.addUrlPatterns("/*");
        return registration;
    }
}
