package pl.wizytowka.config.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.support.TestcontainersConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SecurityIntegrationTest.PingController.class})
@TestPropertySource(properties = {
        "showroom.security.jwt.secret=" + SecurityIntegrationTest.SECRET,
        "showroom.security.bootstrap-admin.email=" + SecurityIntegrationTest.ADMIN_EMAIL,
        "showroom.security.bootstrap-admin.password=" + SecurityIntegrationTest.ADMIN_PASSWORD,
        "showroom.storage.local-root=target/test-storage",
        // test celowo loguje się ~20 razy w kilka sekund; limity sprawdza RateLimitIntegrationTest
        "showroom.rate-limit.enabled=false"
})
class SecurityIntegrationTest {

    static final String SECRET = "c2hvd3Jvb20tdGVzdC1zZWNyZXQtd2hpY2gtaXMtbG9uZy1lbm91Z2gtZm9yLWhzMjU2";
    static final String ADMIN_EMAIL = "admin@showroom.test";
    static final String ADMIN_PASSWORD = "bootstrap admin password";
    private static final String COOKIE = "showroom_refresh";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtEncoder jwtEncoder;

    // ------------------------------------------------------------------ reguły dostępu

    @Test
    void publicPathsAreOpenAndAdminPathsRequireToken() throws Exception {
        mvc.perform(get("/api/v1/catalog/ping")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("auth.unauthenticated"));
    }

    @Test
    void unknownPathsAreDeniedEvenWhenAuthenticated() throws Exception {
        mvc.perform(get("/api/v1/unknown")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/catalog/ping")).andExpect(status().isUnauthorized());

        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken;
        mvc.perform(get("/api/v1/unknown").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void validTokenGrantsAccessAndSecurityHeadersArePresent() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken;

        mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
        mvc.perform(get("/api/v1/admin/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void tamperedExpiredOrForeignIssuerTokensAreRejected() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken;
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        Instant now = Instant.now();
        String expired = sign("b2b-showroom", now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
        String foreignIssuer = sign("someone-else", now, now.plus(10, ChronoUnit.MINUTES));

        for (String bad : List.of(tampered, expired, foreignIssuer, "not-a-jwt")) {
            mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + bad))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + sign("b2b-showroom", now, now.plusSeconds(600))))
                .andExpect(status().isOk());
    }

    @Test
    void editorCanUseAdminPanelButNotManageUsers() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken;
        String editorEmail = uniqueEmail("editor");
        createUser(adminToken, editorEmail, "EDITOR", "editor password 123");

        String editorToken = login(editorEmail, "editor password 123").accessToken;

        mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + editorToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.hasItem(editorEmail)));
    }

    // ------------------------------------------------------------------ sesja

    @Test
    void loginSetsHardenedRefreshCookieAndRequiresCsrfHeader() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.csrf-header-missing"));

        Session session = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(session.setCookieHeader)
                .contains(COOKIE + "=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/admin/auth")
                .contains("Max-Age=1209600");
        assertThat(session.refreshToken).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void wrongPasswordAndUnknownUserAreIndistinguishable() throws Exception {
        String wrongPassword = mvc.perform(authPost("/login", null)
                        .content(json(Map.of("email", uniqueEmail("ghost"), "password", "definitely wrong"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(wrongPassword).get("code").asText()).isEqualTo("identity.invalid-credentials");
        mvc.perform(authPost("/login", null).content(json(Map.of("email", "ghost@showroom.test", "password", "whatever pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("identity.invalid-credentials"));
    }

    @Test
    void refreshRotatesTokenAndReuseKillsTheSession() throws Exception {
        Session first = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MockHttpServletResponse rotated = mvc.perform(authPost("/refresh", first.refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse();
        String second = rotated.getCookie(COOKIE).getValue();
        assertThat(second).isNotEqualTo(first.refreshToken);

        mvc.perform(authPost("/refresh", first.refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("identity.invalid-refresh-token"))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        mvc.perform(authPost("/refresh", second)).andExpect(status().isUnauthorized());
        mvc.perform(authPost("/refresh", null)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() throws Exception {
        Session session = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(authPost("/logout", session.refreshToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        mvc.perform(authPost("/refresh", session.refreshToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void accountLocksAfterFiveFailedAttempts() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken;
        String email = uniqueEmail("lockme");
        createUser(adminToken, email, "EDITOR", "the right password");

        for (int i = 0; i < 5; i++) {
            mvc.perform(authPost("/login", null).content(json(Map.of("email", email, "password", "the wrong password"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(authPost("/login", null).content(json(Map.of("email", email, "password", "the right password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("identity.invalid-credentials"));
    }

    @Test
    void disabledUserLosesRefreshButAdminCannotDisableSelf() throws Exception {
        Session admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail("todisable");
        String id = createUser(admin.accessToken, email, "EDITOR", "editor password 123");
        Session editor = login(email, "editor password 123");

        mvc.perform(post("/api/v1/admin/users/" + id + "/disable").header("Authorization", "Bearer " + admin.accessToken))
                .andExpect(status().isNoContent());
        mvc.perform(authPost("/refresh", editor.refreshToken)).andExpect(status().isUnauthorized());

        String adminId = objectMapper.readTree(mvc.perform(get("/api/v1/admin/auth/me")
                        .header("Authorization", "Bearer " + admin.accessToken))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/v1/admin/users/" + adminId + "/disable").header("Authorization", "Bearer " + admin.accessToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("identity.cannot-disable-self"));
    }

    // ------------------------------------------------------------------ pomocnicze

    private record Session(String accessToken, String refreshToken, String setCookieHeader) {
    }

    private Session login(String email, String password) throws Exception {
        MockHttpServletResponse response = mvc.perform(authPost("/login", null)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse();
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        return new Session(body.get("accessToken").asText(), response.getCookie(COOKIE).getValue(),
                response.getHeader("Set-Cookie"));
    }

    private String createUser(String adminToken, String email, String role, String password) throws Exception {
        String response = mvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "displayName", "Test", "role", role, "password", password))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private MockHttpServletRequestBuilder authPost(String path, String refreshToken) {
        MockHttpServletRequestBuilder request = post("/api/v1/admin/auth" + path)
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON);
        if (refreshToken != null) {
            request.cookie(new Cookie(COOKIE, refreshToken));
        }
        return request;
    }

    private String sign(String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("roles", List.of("ADMIN"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@showroom.test";
    }

    @RestController
    static class PingController {

        @GetMapping({"/api/v1/catalog/ping", "/api/v1/admin/ping"})
        Map<String, String> ping() {
            return Map.of("status", "ok");
        }
    }
}
