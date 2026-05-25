package com.react.spring.auth.repository;

import com.react.spring.auth.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Identity-store repository. {@code JpaRepository} is allowed here as the
 * documented exception in rules/data-access.md — this entity is the consumer's
 * own user table and is read by the authentication layer before any
 * SecureDataManager is in scope.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByLogin(String login);
    Optional<AppUser> findByEmail(String email);
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
}
