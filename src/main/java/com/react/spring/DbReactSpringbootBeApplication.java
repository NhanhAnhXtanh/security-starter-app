package com.react.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
    "com.vn.security.core.domain",
    "com.vn.security.core.security.domain",
    "com.react.spring"
})
public class DbReactSpringbootBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbReactSpringbootBeApplication.class, args);
    }

}
