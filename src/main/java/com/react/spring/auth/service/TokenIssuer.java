package com.react.spring.auth.service;

import com.react.spring.auth.config.AppJwtProperties;
import com.react.spring.auth.entity.AppUser;
import com.react.spring.auth.entity.RefreshToken;
import com.react.spring.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues access tokens (JWT) and refresh tokens (opaque, SHA-256 hashed in DB) for AppUsers.
 * Refresh tokens are single-use: {@link #rotate(String)} revokes the row matching the
 * presented token and persists a new one in the same call.
 */
@Service
public class TokenIssuer {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppJwtProperties props;

    public TokenIssuer(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokenRepository, AppJwtProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.props = props;
    }

    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(props.accessTokenTtlSeconds()))
            .subject(user.getLogin())
            .claim("userId", user.getId().toString())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Transactional
    public String issueRefreshToken(String username) {
        byte[] raw = new byte[48];
        RNG.nextBytes(raw);
        String token = URL_ENCODER.encodeToString(raw);

        RefreshToken row = new RefreshToken();
        row.setId(UUID.randomUUID());
        row.setUsername(username);
        row.setTokenHash(sha256(token));
        row.setExpiresAt(Instant.now().plus(props.refreshTokenTtlSeconds(), ChronoUnit.SECONDS));
        row.setRevoked(false);
        refreshTokenRepository.save(row);
        return token;
    }

    /**
     * Consumes {@code presented} and issues a fresh refresh token in one transaction.
     * Returns the new token + username, or empty if the presented one is invalid/expired/revoked.
     */
    @Transactional
    public Optional<RotationResult> rotate(String presented) {
        String hash = sha256(presented);
        Optional<RefreshToken> existing = refreshTokenRepository.findByTokenHash(hash);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken row = existing.get();
        if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        row.setRevoked(true);
        refreshTokenRepository.save(row);
        String fresh = issueRefreshToken(row.getUsername());
        return Optional.of(new RotationResult(row.getUsername(), fresh));
    }

    @Transactional
    public void revokeAll(String username) {
        refreshTokenRepository.revokeAllForUsername(username);
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtlSeconds();
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed on every JVM, so this is unreachable.
            throw new IllegalStateException(e);
        }
    }

    public record RotationResult(String username, String newRefreshToken) {}
}
