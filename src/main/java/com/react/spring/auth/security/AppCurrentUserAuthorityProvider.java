package com.react.spring.auth.security;

import com.react.spring.auth.repository.AppUserRoleRepository;
import com.vn.security.core.security.CurrentUserAuthorityProvider;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer-side implementation of the starter's authority SPI. Given an authenticated
 * username, returns the role names assigned to that user in {@code app_user_role}.
 *
 * <p>The starter's {@code DefaultCurrentUserAuthorityResolver} further validates each name
 * against {@code sec_authority} (dropping phantom roles) and caches the validated result per
 * username in Hazelcast, so this provider only needs to do the raw lookup — it should not
 * try to filter, cache, or transform on its own.
 */
@Component
public class AppCurrentUserAuthorityProvider implements CurrentUserAuthorityProvider {

    private final AppUserRoleRepository userRoleRepository;

    public AppCurrentUserAuthorityProvider(AppUserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> getAuthorities(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return userRoleRepository.findAuthoritiesByUsername(username);
    }
}
