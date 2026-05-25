package com.react.spring.auth.repository;

import com.react.spring.auth.entity.AppUserRole;
import com.react.spring.auth.entity.AppUserRole.AppUserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRoleRepository extends JpaRepository<AppUserRole, AppUserRoleId> {

    @Query("select r.id.authority from AppUserRole r where r.id.username = :username")
    List<String> findAuthoritiesByUsername(@Param("username") String username);

    List<AppUserRole> findAllByIdUsername(String username);

    void deleteAllByIdUsername(String username);
}
