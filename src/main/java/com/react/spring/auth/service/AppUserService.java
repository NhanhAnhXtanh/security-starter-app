package com.react.spring.auth.service;

import com.react.spring.auth.entity.AppUser;
import com.react.spring.auth.entity.AppUserRole;
import com.react.spring.auth.repository.AppUserRepository;
import com.react.spring.auth.repository.AppUserRoleRepository;
import com.vn.security.core.security.UserAuthorityCacheService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application user lifecycle. Every write path that changes role membership or password
 * invokes {@code UserAuthorityCacheService.evict(username)} so the next authenticated
 * request re-reads through the starter's CurrentUserAuthorityProvider.
 */
@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final AppUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthorityCacheService authorityCache;

    public AppUserService(
        AppUserRepository userRepository,
        AppUserRoleRepository userRoleRepository,
        PasswordEncoder passwordEncoder,
        UserAuthorityCacheService authorityCache
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityCache = authorityCache;
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByLogin(String login) {
        return userRepository.findByLogin(login == null ? null : login.toLowerCase());
    }

    @Transactional(readOnly = true)
    public boolean existsByLogin(String login) {
        return login != null && userRepository.existsByLogin(login.toLowerCase());
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmail(email.toLowerCase());
    }

    @Transactional
    public AppUser register(String login, String rawPassword, String email, String firstName, String lastName, String defaultRole) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActivated(true);
        userRepository.save(user);

        if (defaultRole != null && !defaultRole.isBlank()) {
            userRoleRepository.save(new AppUserRole(user.getLogin(), defaultRole));
        }

        // Evict so the very first authenticated request sees the role we just inserted —
        // the starter's write-path eviction only covers admin role/permission changes.
        authorityCache.evict(user.getLogin());
        return user;
    }

    @Transactional
    public void changePassword(String login, String newRawPassword) {
        AppUser user = userRepository.findByLogin(login.toLowerCase()).orElseThrow();
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        // Password change is not a role change, but evict as defense-in-depth so any cached
        // entry tied to a soon-to-be-revoked session is dropped at the same moment.
        authorityCache.evict(user.getLogin());
    }
}
