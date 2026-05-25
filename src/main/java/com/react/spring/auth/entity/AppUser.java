package com.react.spring.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.react.spring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Consumer-owned application user. Extends the project's BaseEntity (which the rest of the
 * domain already uses) rather than the starter's SecurityUser, so that auditing/versioning
 * stays consistent across entities. Implements UserDetails directly so the standard
 * DaoAuthenticationProvider can authenticate against it.
 *
 * <p>Role membership lives in {@link AppUserRole}, not on this entity, so the starter resolves
 * authorities through {@code CurrentUserAuthorityProvider} keyed by username — never by
 * traversing this user record.
 */
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity implements UserDetails {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9._-]{3,50}$")
    @Size(min = 3, max = 50)
    @Column(name = "login", length = 50, unique = true, nullable = false)
    private String login;

    @JsonIgnore
    @NotBlank
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", length = 60, nullable = false)
    private String password;

    @Email
    @Size(max = 191)
    @Column(name = "email", length = 191, unique = true)
    private String email;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "activated", nullable = false)
    private boolean activated = true;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        // Normalize to lowercase so login lookups are case-insensitive without a functional index.
        this.login = login == null ? null : login.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase(Locale.ENGLISH);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Authority resolution is the starter's job via CurrentUserAuthorityProvider.
        // Returning empty here keeps any accidental getAuthorities() call from leaking
        // stale or wrong roles into the Spring authentication.
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activated;
    }
}
