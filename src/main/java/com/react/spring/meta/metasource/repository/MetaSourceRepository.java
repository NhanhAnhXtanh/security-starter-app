package com.react.spring.meta.metasource.repository;

import com.react.spring.meta.metasource.entity.MetaSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaSourceRepository extends JpaRepository<MetaSource, UUID> {

    Optional<MetaSource> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query(value = "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM core_meta_source WHERE code ~ '^[0-9]+$'", nativeQuery = true)
    int findMaxNumericCode();

    List<MetaSource> findByEnabledTrue();
}
