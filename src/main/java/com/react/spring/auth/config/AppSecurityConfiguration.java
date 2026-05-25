package com.react.spring.auth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.react.spring.auth.security.AppUserDetailsService;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT-stateless security configuration owned by this consumer. The starter no longer ships
 * a SecurityFilterChain in v0.1.0 — it only contributes authorization helpers that read
 * {@code Authentication.getName()}. This class wires:
 *
 * <ul>
 *   <li>the filter chain (permit /api/auth/**, lock /api/admin/** to ROLE_ADMIN, require
 *       authentication elsewhere);</li>
 *   <li>JwtEncoder + JwtDecoder (HS512, shared secret from {@code app.jwt.base64-secret});</li>
 *   <li>AuthenticationManager backed by {@link AppUserDetailsService};</li>
 *   <li>a JwtAuthenticationConverter that explicitly drops JWT-embedded authorities so the
 *       starter's CurrentUserAuthorityProvider remains the only source of role truth.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AppJwtProperties.class)
public class AppSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password/**"
                ).permitAll()
                .requestMatchers("/management/health/**", "/management/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            ))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                .accessDeniedHandler((req, res, ex) -> res.sendError(403))
            )
            .build();
    }

    /**
     * Force the JWT to contribute zero authorities. The starter's
     * DefaultCurrentUserAuthorityResolver will then go through CurrentUserAuthorityProvider
     * for every request — that is exactly the username-only flow we want, and it keeps a
     * stale 'auth' claim on a long-lived token from outranking a fresh DB read.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(AppJwtProperties props) {
        byte[] keyBytes = Base64.getDecoder().decode(props.base64Secret());
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(AppJwtProperties props) {
        byte[] keyBytes = Base64.getDecoder().decode(props.base64Secret());
        OctetSequenceKey key = new OctetSequenceKey.Builder(keyBytes).algorithm(JWSAlgorithm.HS512).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
