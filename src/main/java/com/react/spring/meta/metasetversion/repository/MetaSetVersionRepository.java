package com.react.spring.meta.metasetversion.repository;

import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaSetVersionRepository extends JpaRepository<MetaSetVersion, UUID> {

    List<MetaSetVersion> findByMetaCodeOrderByVersionNoDesc(String metaCode);

    Optional<MetaSetVersion> findByMetaCodeAndVersionNo(String metaCode, Integer versionNo);

    // Find all versions linked to a MetaSync by metasyncCode
    List<MetaSetVersion> findByMetasyncCode(String metasyncCode);

    // Delete all versions linked to a MetaSync (cascade cleanup)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MetaSetVersion v WHERE v.metasyncCode = :metasyncCode")
    int deleteByMetasyncCode(@Param("metasyncCode") String metasyncCode);
}
