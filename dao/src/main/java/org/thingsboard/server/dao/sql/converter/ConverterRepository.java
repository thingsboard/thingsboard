// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.converter;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edqs.fields.ConverterFields;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.ConverterEntity;

import java.util.List;
import java.util.UUID;

public interface ConverterRepository extends JpaRepository<ConverterEntity, UUID>, ExportableEntityRepository<ConverterEntity> {

    @Query("SELECT a FROM ConverterEntity a WHERE a.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ConverterEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT a FROM ConverterEntity a WHERE a.tenantId = :tenantId " +
            "AND a.edgeTemplate = :isEdgeTemplate " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true) " +
            "AND (a.integrationType IS NULL OR :integrationType IS NULL OR a.integrationType = :integrationType)")
    Page<ConverterEntity> findByTenantIdAndIsEdgeTemplate(@Param("tenantId") UUID tenantId,
                                                          @Param("searchText") String searchText,
                                                          @Param("isEdgeTemplate") boolean isEdgeTemplate,
                                                          @Param("integrationType") IntegrationType integrationType,
                                                          Pageable pageable);

    ConverterEntity findByTenantIdAndName(UUID tenantId, String name);

    ConverterEntity findByTenantIdAndNameAndType(UUID tenantId, String name, ConverterType type);

    @Query("SELECT count(c) > 0 FROM ConverterEntity c WHERE c.tenantId = :tenantId " +
            "AND c.name = :name AND c.type = :type AND (:skippedId IS NULL OR c.id <> :skippedId)")
    boolean existsByTenantIdAndNameAndTypeAndIdNot(@Param("tenantId") UUID tenantId,
                                                   @Param("name") String name,
                                                   @Param("type") ConverterType type,
                                                   @Param("skippedId") UUID skippedId);

    List<ConverterEntity> findConvertersByTenantIdAndIdIn(UUID tenantId, List<UUID> converterIds);

    Long countByTenantId(UUID tenantId);

    Long countByTenantIdAndEdgeTemplateFalse(UUID tenantId);

    @Query("SELECT externalId FROM ConverterEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT c.integrationType, c.type FROM ConverterEntity c WHERE c.tenantId = :tenantId GROUP BY c.integrationType, c.type")
    List<Object[]> findExistingConverterTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.ConverterFields(c.id, c.createdTime, c.tenantId, " +
            "c.name, c.version, c.type, c.additionalInfo) FROM ConverterEntity c WHERE c.id > :id ORDER BY c.id")
    List<ConverterFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query(value = "SELECT COUNT(*) FROM converter WHERE configuration::json ->> 'scriptLang' = :scriptLang", nativeQuery = true)
    Long countByScriptLang(String scriptLang);

    Long countAllByIntegrationTypeIsNull();

    Long countAllByIntegrationTypeIsNotNull();

    Long countAllByConverterVersionAndIntegrationTypeIsNotNull(Integer converterVersion);

}
