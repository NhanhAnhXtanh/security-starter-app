package com.react.spring.catalog.repository;

import com.react.spring.catalog.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {
}
