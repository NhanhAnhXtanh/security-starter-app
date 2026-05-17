package com.react.spring.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Workaround for starter's AsyncConfiguration being incompatible with Spring Boot 4 —
 * the parent class signature changed and reflection invocation throws
 * "Illegal factory instance for factory method 'getAsyncExecutor'".
 *
 * Override the taskExecutor bean here. Requires
 * spring.main.allow-bean-definition-overriding=true in application.yml.
 */
@Configuration
@EnableAsync
public class AsyncOverrideConfig {

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
