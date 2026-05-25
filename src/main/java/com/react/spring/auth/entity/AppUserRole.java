package com.react.spring.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * User → role assignment, owned by the consumer. The role name on this side must match a
 * row in the starter's {@code sec_authority} table; {@code CurrentUserAuthorityProvider}
 * joins on it to resolve authorities.
 *
 * <p>Composite primary key (username + authority) so the same user can hold multiple roles
 * and the same role can be assigned to many users without an extra surrogate id.
 */
@Entity
@Table(name = "app_user_role")
public class AppUserRole {

    @EmbeddedId
    private AppUserRoleId id;

    public AppUserRole() {}

    public AppUserRole(String username, String authority) {
        this.id = new AppUserRoleId(username, authority);
    }

    public AppUserRoleId getId() {
        return id;
    }

    public void setId(AppUserRoleId id) {
        this.id = id;
    }

    public String getUsername() {
        return id == null ? null : id.getUsername();
    }

    public String getAuthority() {
        return id == null ? null : id.getAuthority();
    }

    @Embeddable
    public static class AppUserRoleId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank
        @Size(max = 50)
        @Column(name = "username", length = 50, nullable = false)
        private String username;

        @NotBlank
        @Size(max = 50)
        @Column(name = "authority", length = 50, nullable = false)
        private String authority;

        public AppUserRoleId() {}

        public AppUserRoleId(String username, String authority) {
            this.username = username;
            this.authority = authority;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AppUserRoleId other)) return false;
            return Objects.equals(username, other.username) && Objects.equals(authority, other.authority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, authority);
        }
    }
}
