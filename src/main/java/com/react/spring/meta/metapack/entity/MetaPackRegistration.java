package com.react.spring.meta.metapack.entity;

import com.vn.security.core.security.catalog.SecuredEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@SecuredEntity(jpqlAllowed = true)
@Entity
@Table(name = "META_PACK_REGISTRATION")
@EntityListeners(AuditingEntityListener.class)
public class MetaPackRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "META_PACK_ID", nullable = false)
    private MetaPack metaPack;

    @Column(name = "SUBSCRIBER_NAME", nullable = false)
    private String subscriberName;

    @Column(name = "REQUESTED_FIELDS", columnDefinition = "TEXT")
    private String requestedFields; // JSON list of field paths

    @Column(name = "API_KEY", unique = true)
    private String apiKey;

    @Column(name = "API_SETTINGS", columnDefinition = "TEXT")
    private String apiSettings; // JSON or text containing provided settings

    @Column(name = "STATUS")
    private String status; // PENDING, APPROVED, REJECTED, REVOKED

    @Column(name = "CUSTOM_RATE_LIMIT_PM")
    private Integer customRateLimitPerMinute;

    @Column(name = "CUSTOM_RATE_LIMIT_PD")
    private Integer customRateLimitPerDay;

    @Column(name = "EXPIRES_AT")
    private OffsetDateTime expiresAt;

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedBy
    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MetaPack getMetaPack() { return metaPack; }
    public void setMetaPack(MetaPack metaPack) { this.metaPack = metaPack; }

    public String getSubscriberName() { return subscriberName; }
    public void setSubscriberName(String subscriberName) { this.subscriberName = subscriberName; }

    public String getRequestedFields() { return requestedFields; }
    public void setRequestedFields(String requestedFields) { this.requestedFields = requestedFields; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSettings() { return apiSettings; }
    public void setApiSettings(String apiSettings) { this.apiSettings = apiSettings; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCustomRateLimitPerMinute() { return customRateLimitPerMinute; }
    public void setCustomRateLimitPerMinute(Integer customRateLimitPerMinute) { this.customRateLimitPerMinute = customRateLimitPerMinute; }

    public Integer getCustomRateLimitPerDay() { return customRateLimitPerDay; }
    public void setCustomRateLimitPerDay(Integer customRateLimitPerDay) { this.customRateLimitPerDay = customRateLimitPerDay; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
