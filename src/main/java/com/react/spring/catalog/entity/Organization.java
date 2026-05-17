package com.react.spring.catalog.entity;

import com.react.spring.common.entity.BaseEntity;
import com.vn.security.core.security.catalog.SecuredEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

// Starter also ships com.vn.security.core.domain.Organization (demo entity).
// Override JPA entity name + secured-entity code so both can coexist.
@SecuredEntity(code = "app-organization")
@Entity(name = "AppOrganization")
@Table(name = "core_organization")
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
