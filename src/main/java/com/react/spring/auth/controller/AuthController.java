package com.react.spring.auth.controller;

import com.react.spring.auth.entity.AppUser;
import com.react.spring.auth.service.AppUserService;
import com.react.spring.auth.service.TokenIssuer;
import com.react.spring.auth.service.TokenIssuer.RotationResult;
import com.react.spring.common.exception.NotFoundException;
import com.vn.security.core.security.UserAuthorityCacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final AuthenticationManager authenticationManager;
    private final AppUserService userService;
    private final TokenIssuer tokenIssuer;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthorityCacheService authorityCache;

    public AuthController(
        AuthenticationManager authenticationManager,
        AppUserService userService,
        TokenIssuer tokenIssuer,
        PasswordEncoder passwordEncoder,
        UserAuthorityCacheService authorityCache
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenIssuer = tokenIssuer;
        this.passwordEncoder = passwordEncoder;
        this.authorityCache = authorityCache;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username().toLowerCase(), req.password())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SecurityContextHolder.getContext().setAuthentication(auth);

        AppUser user = userService.findByLogin(auth.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Pre-warm the authority cache so the user's first authenticated request after login
        // does not pay the AppCurrentUserAuthorityProvider DB hit.
        authorityCache.warmUp(user.getLogin());

        String accessToken = tokenIssuer.issueAccessToken(user);
        String refreshToken = tokenIssuer.issueRefreshToken(user.getLogin());
        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, tokenIssuer.accessTokenTtlSeconds()));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.existsByLogin(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("X-Reason", "loginused").build();
        }
        if (req.email() != null && userService.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("X-Reason", "emailused").build();
        }
        userService.register(req.username(), req.password(), req.email(), req.firstName(), req.lastName(), DEFAULT_ROLE);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        RotationResult rotated = tokenIssuer.rotate(req.refreshToken())
            .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        AppUser user = userService.findByLogin(rotated.username())
            .orElseThrow(() -> new NotFoundException("User not found"));
        String accessToken = tokenIssuer.issueAccessToken(user);
        return ResponseEntity.ok(new TokenResponse(accessToken, rotated.newRefreshToken(), tokenIssuer.accessTokenTtlSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication auth) {
        if (auth != null && auth.getName() != null) {
            tokenIssuer.revokeAll(auth.getName());
            authorityCache.evict(auth.getName());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        AppUser user = userService.findByLogin(auth.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Reason", "wrongpassword").build();
        }
        userService.changePassword(user.getLogin(), req.newPassword());
        // Sign out everywhere on password change — defense in depth in case the old
        // credentials were compromised.
        tokenIssuer.revokeAll(user.getLogin());
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank String password
    ) {}

    public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9._-]{3,50}$") String username,
        @NotBlank @Size(min = 4, max = 100) String password,
        @Email String email,
        String firstName,
        String lastName
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 4, max = 100) String newPassword
    ) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
}
