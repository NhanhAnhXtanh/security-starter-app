package com.react.spring.meta.metapack.entity;

import com.react.spring.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "meta_pack")
public class MetaPack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private String status; // DRAFT, PUBLISHED, DISCONTINUED

    @Column(name = "max_requests_per_minute")
    private Integer maxRequestsPerMinute;

    @Column(name = "max_requests_per_day")
    private Integer maxRequestsPerDay;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "current_version_id")
    private MetaPackVersion currentVersion;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public void setMaxRequestsPerMinute(Integer maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }

    public Integer getMaxRequestsPerDay() { return maxRequestsPerDay; }
    public void setMaxRequestsPerDay(Integer maxRequestsPerDay) { this.maxRequestsPerDay = maxRequestsPerDay; }

    public MetaPackVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(MetaPackVersion currentVersion) { this.currentVersion = currentVersion; }
}
