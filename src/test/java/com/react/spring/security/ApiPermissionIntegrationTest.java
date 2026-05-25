package com.react.spring.security;

import com.react.spring.auth.entity.AppUser;
import com.react.spring.auth.entity.AppUserRole;
import com.react.spring.auth.repository.AppUserRepository;
import com.react.spring.auth.repository.AppUserRoleRepository;
import com.vn.security.core.domain.Authority;
import com.vn.security.core.domain.RoleType;
import com.vn.security.core.repository.AuthorityRepository;
import com.vn.security.core.security.AuthoritiesConstants;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Verifies that the starter's authorization stack (CurrentUserAuthorityProvider ->
 * sec_authority validation -> permission matrix) wires correctly to the consumer's
 * app_user / app_user_role tables.
 *
 * <p>Unlike the pre-v0.1.0 version of this test, the JWT carries only the subject — no
 * 'auth' claim — because AppSecurityConfiguration's JwtAuthenticationConverter drops
 * embedded authorities. Role membership must therefore come from app_user_role.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiPermissionIntegrationTest {

    private static final String API_USER = "api-permission-user";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private AppUserRepository userRepository;
    @Autowired private AppUserRoleRepository userRoleRepository;
    @Autowired private AuthorityRepository authorityRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void ensureUserAndRoleExist() {
        // ROLE_USER must exist in sec_authority — DefaultCurrentUserAuthorityResolver drops
        // any name that has no row there.
        authorityRepository.findById(AuthoritiesConstants.USER).orElseGet(() -> {
            Authority a = new Authority();
            a.setName(AuthoritiesConstants.USER);
            a.setDisplayName("User");
            a.setType(RoleType.RESOURCE);
            return authorityRepository.save(a);
        });

        userRepository.findByLogin(API_USER).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setId(UUID.randomUUID());
            user.setLogin(API_USER);
            user.setPassword(passwordEncoder.encode("test-password"));
            user.setFirstName("API");
            user.setLastName("Permission");
            user.setEmail(API_USER + "@localhost");
            user.setActivated(true);
            return userRepository.save(user);
        });

        if (!userRoleRepository.findAuthoritiesByUsername(API_USER).contains(AuthoritiesConstants.USER)) {
            userRoleRepository.save(new AppUserRole(API_USER, AuthoritiesConstants.USER));
        }
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
                .header("Authorization", bearer(API_USER))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(OK.value(), response.statusCode());
    }

    @Test
    void roleUserCannotCreateEntityWithoutStarterCreatePermission() throws Exception {
        HttpResponse<String> response = httpClient.send(
            HttpRequest.newBuilder(URI.create(url("/api/tags")))
                .header("Authorization", bearer(API_USER))
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

    private String bearer(String subject) {
        return "Bearer " + jwtToken(subject);
    }

    /**
     * Token carries only the subject. The starter's CurrentUserAuthorityProvider resolves
     * authorities from app_user_role at request time, so this mirrors how the production
     * AuthController issues tokens.
     */
    private String jwtToken(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(subject)
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
