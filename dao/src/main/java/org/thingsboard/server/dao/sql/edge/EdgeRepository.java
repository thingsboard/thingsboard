// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.EdgeFields;
import org.thingsboard.server.dao.model.sql.EdgeEntity;

import java.util.List;
import java.util.UUID;

public interface EdgeRepository extends JpaRepository<EdgeEntity, UUID> {

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query(value = "SELECT * FROM edge_active_attribute_view edge_active",
            countQuery = "SELECT count(*) FROM edge_active_attribute_view",
            nativeQuery = true)
    Page<EdgeEntity> findActiveEdges(Pageable pageable);

    @Query("SELECT d.id FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId,
                                 @Param("textSearch") String textSearch,
                                 Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("textSearch") String textSearch,
                                    Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                           @Param("type") String type,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT ee FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toId = :entityId AND re.toType = :entityType " +
            "AND (:textSearch IS NULL OR ilike(ee.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               @Param("entityType") String entityType,
                                               @Param("relationType") String relationType,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT DISTINCT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toId IN (:entityIds) AND re.toType = :entityType")
    Page<UUID> findEdgeIdsByTenantIdAndEntityIds(@Param("tenantId") UUID tenantId,
                                                 @Param("entityIds") List<UUID> entityIds,
                                                 @Param("entityType") String entityType,
                                                 @Param("relationType") String relationType,
                                                 Pageable pageable);

    @Query("SELECT DISTINCT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toType = 'ENTITY_GROUP' " +
            "AND re.toId IN (" +
            "   SELECT fromId FROM RelationEntity " +
            "   WHERE toId = :entityId AND toType = :entityType AND fromType = 'ENTITY_GROUP' " +
            "   AND relationType = 'Contains' AND relationTypeGroup = 'FROM_ENTITY_GROUP')")
    Page<UUID> findEdgeIdsByTenantIdAndGroupEntityId(@Param("tenantId") UUID tenantId,
                                                     @Param("entityId") UUID entityId,
                                                     @Param("entityType") String entityType,
                                                     @Param("relationType") String relationType,
                                                     Pageable pageable);

    @Query("SELECT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.toId = :entityId AND re.toType = :entityType " +
            "AND (:textSearch IS NULL OR ilike(ee.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<UUID> findIdsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                            @Param("entityId") UUID entityId,
                                            @Param("entityType") String entityType,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT ee FROM EdgeEntity ee, TenantEntity te WHERE ee.tenantId = te.id AND te.tenantProfileId = :tenantProfileId ")
    Page<EdgeEntity> findByTenantProfileId(@Param("tenantProfileId") UUID tenantProfileId,
                                           Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM EdgeEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantEdgeTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT count(*) FROM EdgeEntity e WHERE e.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    EdgeEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EdgeEntity> findEdgesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    List<EdgeEntity> findEdgesByTenantIdAndIdIn(UUID tenantId, List<UUID> edgeIds);

    EdgeEntity findByRoutingKey(String routingKey);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND e.type = :type " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.EdgeFields(e.id, e.createdTime, e.tenantId, e.customerId," +
            "e.name, e.version, e.type, e.label, e.additionalInfo) FROM EdgeEntity e WHERE e.id > :id ORDER BY e.id")
    List<EdgeFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
