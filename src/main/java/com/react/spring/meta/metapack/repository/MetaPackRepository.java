package com.react.spring.meta.metapack.repository;

import com.react.spring.meta.metapack.entity.MetaPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaPackRepository extends JpaRepository<MetaPack, UUID> {
    Optional<MetaPack> findByCode(String code);

    boolean existsByCode(String code);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM meta_pack WHERE code ~ '^[0-9]+$'", nativeQuery = true)
    int findMaxNumericCode();
}
