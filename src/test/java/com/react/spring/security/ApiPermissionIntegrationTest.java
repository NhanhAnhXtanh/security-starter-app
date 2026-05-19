package com.react.spring.security;

import com.vn.security.core.security.SecurityUtils;
import com.vn.security.core.domain.Authority;
import com.vn.security.core.domain.User;
import com.vn.security.core.repository.AuthorityRepository;
import com.vn.security.core.repository.UserRepository;
import com.vn.security.core.security.AuthoritiesConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiPermissionIntegrationTest {

    private static final String API_USER = "api-permission-user";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void ensureRoleUserExists() {
        userRepository.findOneByLogin(API_USER).orElseGet(() -> {
            Authority userAuthority = authorityRepository.findById(AuthoritiesConstants.USER)
                    .orElseThrow();
            User user = new User();
            user.setLogin(API_USER);
            user.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoOHi/yIulks25kW3T2WbHkqP2UGoN8W6W");
            user.setFirstName("API");
            user.setLastName("Permission");
            user.setEmail(API_USER + "@localhost");
            user.setActivated(true);
            user.setLangKey("en");
            user.setAuthorities(Set.of(userAuthority));
            return userRepository.save(user);
        });
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(url("/api/tags"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(UNAUTHORIZED.value(), response.statusCode());
    }

    @Test
    void roleUserCanReadEntityAllowedByStarterPermissions() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(url("/api/tags")))
                        .header("Authorization", bearer(API_USER, "ROLE_USER"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(OK.value(), response.statusCode());
    }

    @Test
    void roleUserCannotCreateEntityWithoutStarterCreatePermission() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(url("/api/tags")))
                        .header("Authorization", bearer(API_USER, "ROLE_USER"))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "api-permission-test",
                          "description": "created by permission integration test"
                        }
                        """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(FORBIDDEN.value(), response.statusCode());
    }

    private String bearer(String subject, String authorities) {
        return "Bearer " + jwtToken(subject, authorities);
    }

    private String jwtToken(String subject, String authorities) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim(SecurityUtils.AUTHORITIES_CLAIM, authorities)
                .build();
        JwsHeader header = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
