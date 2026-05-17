package com.react.spring.meta.metapack.repository;

import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaPackRegistrationRepository extends JpaRepository<MetaPackRegistration, UUID> {
    List<MetaPackRegistration> findByMetaPackId(UUID metaPackId);
    Optional<MetaPackRegistration> findByApiKey(String apiKey);
}
