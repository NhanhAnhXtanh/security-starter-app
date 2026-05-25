package com.react.spring.auth.security;

import com.react.spring.auth.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up AppUser by login for DaoAuthenticationProvider. The returned UserDetails reports
 * no authorities — the starter's CurrentUserAuthorityProvider supplies them at request time —
 * but it does honor the {@code activated} flag through AppUser.isEnabled().
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Empty username");
        }
        String normalized = username.toLowerCase();
        return userRepository.findByLogin(normalized)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
    }
}
