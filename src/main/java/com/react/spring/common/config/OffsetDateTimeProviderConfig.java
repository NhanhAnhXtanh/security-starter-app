package com.react.spring.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * BaseEntity audit columns (createdDate, lastModifiedDate) are typed as
 * OffsetDateTime. Spring Data's default DateTimeProvider returns LocalDateTime,
 * which AuditingEntityListener cannot convert to OffsetDateTime — POST/PUT
 * fail with "Cannot convert unsupported date type java.time.LocalDateTime".
 *
 * Marked @Primary so it wins over any DateTimeProvider the starter may declare.
 */
@Configuration
public class OffsetDateTimeProviderConfig {

    @Bean
    @Primary
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
