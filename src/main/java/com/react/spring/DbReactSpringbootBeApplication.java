package com.react.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
    "com.vn.security.core.domain",
    "com.vn.security.core.security.domain",
    "com.react.spring"
})
// Starter's auto-config narrows JPA repository scanning to its own package, which leaves
// AppUserRepository / AppUserRoleRepository / RefreshTokenRepository invisible. Override
// with an explicit scan that covers both — listing the starter's package first keeps its
// AuthorityRepository wiring intact.
@EnableJpaRepositories(basePackages = {
    "com.vn.security.core.repository",
    "com.react.spring"
})
public class DbReactSpringbootBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbReactSpringbootBeApplication.class, args);
    }

}
