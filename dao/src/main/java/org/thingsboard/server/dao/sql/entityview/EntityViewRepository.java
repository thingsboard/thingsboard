// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.EntityViewFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;

import java.util.List;
import java.util.UUID;

public interface EntityViewRepository extends JpaRepository<EntityViewEntity, UUID>, ExportableEntityRepository<EntityViewEntity> {

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityViewEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.type = :type " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityViewEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(e.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                       @Param("customerId") UUID customerId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.type = :type " +
            "AND (:searchText IS NULL OR ilike(e.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("type") String type,
                                                              @Param("searchText") String searchText,
                                                              Pageable pageable);

    // First-match (not strict single-result): under Citus the (tenant_id, name) UNIQUE constraint is dropped,
    // so a select-then-insert race can momentarily leave duplicate rows. Returning the first match converts the
    // resulting IncorrectResultSizeDataAccessException (HTTP 500) into the intended DataValidationException (400);
    // it does not fix the inherent race (best-effort). Plain mode keeps the constraint, so at most one row matches.
    EntityViewEntity findFirstByTenantIdAndName(UUID tenantId, String name);

    @Override
    @Query(value = "SELECT * FROM entity_view WHERE tenant_id = :tenantId AND external_id = :externalId LIMIT 1", nativeQuery = true)
    EntityViewEntity findByTenantIdAndExternalId(@Param("tenantId") UUID tenantId, @Param("externalId") UUID externalId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ENTITY_VIEW', a.name) " +
            "FROM EntityViewEntity a WHERE a.tenantId = :tenantId AND a.name LIKE CONCAT(:prefix, '%')")
    List<EntityInfo> findEntityInfosByNamePrefix(UUID tenantId, String prefix);

    List<EntityViewEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    @Query("SELECT DISTINCT ev.type FROM EntityViewEntity ev WHERE ev.tenantId = :tenantId")
    List<String> findTenantEntityViewTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityViewEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityViewEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND e.type = :type " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityViewEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("type") String type,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    List<EntityViewEntity> findEntityViewsByTenantIdAndIdIn(UUID tenantId, List<UUID> entityViewIds);

    @Query("SELECT ev FROM EntityViewEntity ev, RelationEntity re WHERE ev.tenantId = :tenantId " +
            "AND ev.id = re.toId AND re.toType = 'ENTITY_VIEW' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(ev.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                               @Param("edgeId") UUID edgeId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT ev FROM EntityViewEntity ev, RelationEntity re WHERE ev.tenantId = :tenantId " +
            "AND ev.id = re.toId AND re.toType = 'ENTITY_VIEW' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND ev.type = :type " +
            "AND (:searchText IS NULL OR ilike(ev.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                   @Param("edgeId") UUID edgeId,
                                                   @Param("type") String type,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT id FROM EntityViewEntity WHERE tenantId = :tenantId " +
            "AND (customerId IS NULL OR customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId,
                                                  Pageable pageable);

    @Query("SELECT id FROM EntityViewEntity WHERE tenantId = :tenantId " +
            "AND customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    @Query("SELECT externalId FROM EntityViewEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.EntityViewFields(e.id, e.createdTime, e.tenantId," +
            "e.customerId, e.name, e.type, e.additionalInfo, e.version) FROM EntityViewEntity e WHERE e.id > :id ORDER BY e.id")
    List<EntityViewFields> findNextBatch(@Param("id") UUID id, Limit limit);
}
