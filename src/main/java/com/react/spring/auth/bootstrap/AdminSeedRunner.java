package com.react.spring.auth.bootstrap;

import com.react.spring.auth.config.AppSeedProperties;
import com.react.spring.auth.entity.AppUserRole;
import com.react.spring.auth.repository.AppUserRoleRepository;
import com.react.spring.auth.service.AppUserService;
import com.vn.security.core.domain.Authority;
import com.vn.security.core.domain.RoleType;
import com.vn.security.core.repository.AuthorityRepository;
import com.vn.security.core.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the starter's removed SecurityCoreSeedRunner. Boots an admin/admin user in dev so
 * smoke-test.sh and onboarding flows still have a working login out of the box.
 *
 * <p>Also seeds {@code ROLE_ADMIN} and {@code ROLE_USER} into {@code sec_authority} if absent,
 * because the starter's {@code DefaultCurrentUserAuthorityResolver} drops any authority name
 * not backed by a row there — without these two seeds the admin user would log in but resolve
 * to zero authorities.
 *
 * <p>Gated by {@code app.seed.enabled} — keep this false in production.
 */
@Configuration
@EnableConfigurationProperties(AppSeedProperties.class)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class AdminSeedRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSeedRunner.class);

    @Bean
    public ApplicationRunner seedAdminRunner(
        AppSeedProperties seedProps,
        AppUserService userService,
        AppUserRoleRepository userRoleRepository,
        AuthorityRepository authorityRepository
    ) {
        return args -> seed(seedProps, userService, userRoleRepository, authorityRepository);
    }

    @Transactional
    void seed(
        AppSeedProperties seedProps,
        AppUserService userService,
        AppUserRoleRepository userRoleRepository,
        AuthorityRepository authorityRepository
    ) {
        ensureAuthority(authorityRepository, AuthoritiesConstants.ADMIN, "Administrator");
        ensureAuthority(authorityRepository, AuthoritiesConstants.USER, "User");

        if (userService.existsByLogin(seedProps.username())) {
            LOG.info("Admin seed: user '{}' already exists, skipping creation", seedProps.username());
            ensureAdminRole(userRoleRepository, seedProps.username());
            return;
        }

        userService.register(
            seedProps.username(),
            seedProps.password(),
            seedProps.email(),
            "Admin",
            "User",
            AuthoritiesConstants.ADMIN
        );
        LOG.warn("Admin seed: created '{}' with default password. Disable app.seed.enabled in production.", seedProps.username());
    }

    private void ensureAuthority(AuthorityRepository repo, String name, String displayName) {
        if (repo.findById(name).isPresent()) {
            return;
        }
        Authority a = new Authority();
        a.setName(name);
        a.setDisplayName(displayName);
        a.setType(RoleType.RESOURCE);
        repo.save(a);
        LOG.info("Admin seed: inserted authority '{}'", name);
    }

    private void ensureAdminRole(AppUserRoleRepository roleRepo, String username) {
        boolean hasAdmin = roleRepo.findAuthoritiesByUsername(username.toLowerCase()).contains(AuthoritiesConstants.ADMIN);
        if (!hasAdmin) {
            roleRepo.save(new AppUserRole(username.toLowerCase(), AuthoritiesConstants.ADMIN));
            LOG.info("Admin seed: granted ROLE_ADMIN to existing user '{}'", username);
        }
    }
}
