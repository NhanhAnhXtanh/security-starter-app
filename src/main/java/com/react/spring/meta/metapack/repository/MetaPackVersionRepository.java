package com.react.spring.meta.metapack.repository;

import com.react.spring.meta.metapack.entity.MetaPackVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaPackVersionRepository extends JpaRepository<MetaPackVersion, UUID> {
    List<MetaPackVersion> findByMetaPackIdOrderByVersionNumberDesc(UUID metaPackId);
    Optional<MetaPackVersion> findByMetaPackIdAndVersionNumber(UUID metaPackId, Integer versionNumber);
}
