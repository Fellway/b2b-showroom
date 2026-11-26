package pl.wizytowka.shared.adapter.in.web.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(RateLimitConfiguration.class);

    @Test
    void registersFilterBeforeSpringSecurityByDefault() {
        runner.run(context -> {
            FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
            assertThat(registration.getFilter()).isInstanceOf(RateLimitFilter.class);
            assertThat(registration.getOrder()).isLessThan(
                    org.springframework.boot.autoconfigure.security.SecurityProperties.DEFAULT_FILTER_ORDER);
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("showroom.rate-limit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void failsFastOnInvalidRule() {
        runner.withPropertyValues(
                        "showroom.rate-limit.rules[0].name=broken",
                        "showroom.rate-limit.rules[0].method=POST",
                        "showroom.rate-limit.rules[0].path=no-leading-slash",
                        "showroom.rate-limit.rules[0].limits[0].capacity=1",
                        "showroom.rate-limit.rules[0].limits[0].period=1m")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void applicationYamlDefinesExpectedDefaults() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);

        RateLimitProperties properties = Binder.get(environment)
                .bind("showroom.rate-limit", RateLimitProperties.class).get();

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.rules()).extracting(RateLimitProperties.Rule::path).containsExactly(
                "/api/v1/buyer/line-sheets",
                "/api/v1/buyer/auth/register", "/api/v1/buyer/auth/resend-verification",
                "/api/v1/buyer/auth/password-reset/request", "/api/v1/buyer/auth/verify-email",
                "/api/v1/buyer/auth/password-reset/confirm", "/api/v1/buyer/auth/login", "/api/v1/buyer/auth/refresh",
                "/api/v1/buyer/me/password", "/api/v1/buyer/me/delete", "/api/v1/buyer/me/export",
                "/api/v1/analytics/events", "/api/v1/admin/auth/login", "/api/v1/admin/auth/refresh");
        assertThat(properties.rules().getFirst().limits()).containsExactly(
                new RateLimitProperties.Limit(5, Duration.ofMinutes(1)),
                new RateLimitProperties.Limit(30, Duration.ofHours(1)));
    }
}
