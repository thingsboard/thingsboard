// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.integration;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.IntegrationFields;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;

import java.util.List;
import java.util.UUID;

public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID>, ExportableEntityRepository<IntegrationEntity> {

    @Query("SELECT a FROM IntegrationEntity a WHERE a.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("searchText") String searchText,
                                           Pageable pageable);

    @Query("SELECT a FROM IntegrationEntity a WHERE a.tenantId = :tenantId " +
            "AND a.edgeTemplate = :isEdgeTemplate " +
            "AND (:searchText IS NULL OR ilike(a.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationEntity> findByTenantIdAndIsEdgeTemplate(@Param("tenantId") UUID tenantId,
                                                            @Param("searchText") String searchText,
                                                            @Param("isEdgeTemplate") boolean isEdgeTemplate,
                                                            Pageable pageable);

    IntegrationEntity findByRoutingKey(String routingKey);

    @Query("SELECT a FROM IntegrationEntity a WHERE a.tenantId = :tenantId AND (a.converterId = :converterId OR a.downlinkConverterId = :converterId)")
    List<IntegrationEntity> findByConverterId(@Param("tenantId") UUID tenantId,
                                              @Param("converterId") UUID converterId);

    List<IntegrationEntity> findIntegrationsByTenantIdAndIdIn(UUID tenantId, List<UUID> integrationIds);

    // NOTE: the selected columns bind positionally to the lightweight Integration(UUID, UUID, String, IntegrationType, Boolean, Boolean, Boolean)
    // projection constructor. The order here must match that constructor's parameter order exactly: id, tenantId, name, type, enabled, isRemote, allowCreateDevicesOrAssets.
    // A transposed pair (e.g. the adjacent Booleans) compiles cleanly but silently produces a wrong Integration, so keep the two in lockstep.
    @Query("SELECT new org.thingsboard.server.common.data.integration.Integration(i.id, i.tenantId, i.name, i.type, i.enabled, i.isRemote, i.allowCreateDevicesOrAssets) " +
            "FROM IntegrationEntity i WHERE i.type = :type AND i.isRemote = :isRemote AND i.enabled = :enabled AND i.edgeTemplate = false")
    List<Integration> findCoreIntegrations(@Param("type") IntegrationType type, @Param("isRemote") boolean remote, @Param("enabled") boolean enabled);

    @Query("SELECT ie FROM IntegrationEntity ie, RelationEntity re WHERE ie.tenantId = :tenantId " +
            "AND ie.id = re.toId AND re.toType = 'INTEGRATION' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(ie.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                    @Param("edgeId") UUID edgeId,
                                                    @Param("searchText") String searchText,
                                                    Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(integration.id, 'INTEGRATION', integration.name) " +
            "FROM IntegrationEntity integration WHERE integration.tenantId = :tenantId AND ilike(integration.configuration, CONCAT('%', :placeholder, '%'))")
    List<EntityInfo> findByTenantIdAndSecretPlaceholder(@Param("tenantId") UUID tenantId,
                                                        @Param("placeholder") String placeholder);

    Long countByTenantId(UUID tenantId);

    Long countByTenantIdAndEdgeTemplateFalse(UUID tenantId);

    Long countByEdgeTemplateFalse();

    List<IntegrationEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT externalId FROM IntegrationEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.IntegrationFields(i.id, i.createdTime," +
            "i.tenantId, i.name, i.version, i.type, i.additionalInfo) FROM IntegrationEntity i WHERE i.id > :id ORDER BY i.id")
    List<IntegrationFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query(value = "SELECT new org.thingsboard.server.common.data.util.TbPair(i.type, count(i)) FROM IntegrationEntity i GROUP BY i.type")
    List<TbPair<IntegrationType, Long>> countIntegrationsPerType();

}
