// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityGroupFields;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EntityGroupRepository extends JpaRepository<EntityGroupEntity, UUID> {

    String TENANT_ID_FILTER = "((e.ownerId = :tenantId AND e.ownerType = 'TENANT') OR " +
            "(e.ownerId in (SELECT c.id FROM CustomerEntity c where c.tenantId = :tenantId) and e.ownerType = 'CUSTOMER'))";

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findEntityGroupsByType(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") EntityType parentEntityType,
                                                   @Param("groupType") EntityType groupType,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE " +
            TENANT_ID_FILTER +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findEntityGroupsByType(
            @Param("tenantId") UUID tenantId,
            @Param("groupType") EntityType groupType,
            @Param("textSearch") String textSearch,
            Pageable toPageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND e.name = :name")
    EntityGroupEntity findEntityGroupByTypeAndName(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") EntityType parentEntityType,
                                                   @Param("groupType") EntityType groupType,
                                                   @Param("name") String name);

    @Query("SELECT e FROM EntityGroupEntity e, RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findAllEntityGroupsByParentRelation(@Param("parentEntityId") UUID parentEntityId,
                                                                @Param("parentEntityType") String parentEntityType,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findAllEntityGroups(@Param("parentEntityId") UUID parentEntityId,
                                                @Param("parentEntityType") EntityType parentEntityType,
                                                @Param("textSearch") String textSearch,
                                                Pageable pageable);

    @Query("SELECT re.toId " +
            "FROM RelationEntity re " +
            "WHERE re.toType = :groupType " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP'")
    Page<UUID> findGroupEntityIds(@Param("groupId") UUID groupId,
                                  @Param("groupType") String groupType,
                                  Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :edgeId AND re.fromType = 'EDGE'")
    Page<EntityGroupEntity> findEdgeEntityGroupsByType(@Param("edgeId") UUID edgeId,
                                                       @Param("relationType") String relationType,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND e.ownerId = :ownerId " +
            "AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :edgeId AND re.fromType = 'EDGE'")
    Page<EntityGroupEntity> findEdgeEntityGroupsByOwnerIdAndType(@Param("edgeId") UUID edgeId,
                                                                 @Param("ownerId") UUID ownerId,
                                                                 @Param("relationType") String relationType,
                                                                 Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e WHERE " +
            "e.externalId = :externalId AND " + TENANT_ID_FILTER)
    EntityGroupEntity findByTenantIdAndExternalId(@Param("tenantId") UUID tenantId, @Param("externalId") UUID externalId);

    @Query("SELECT e FROM EntityGroupEntity e WHERE " + TENANT_ID_FILTER)
    Page<EntityGroupEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM EntityGroupEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.EntityGroupFields(eg.id, eg.createdTime, " +
            "eg.name, eg.version, eg.type, eg.additionalInfo, eg.ownerId, eg.ownerType) FROM EntityGroupEntity eg WHERE eg.id > :id ORDER BY eg.id")
    List<EntityGroupFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query("SELECT DISTINCT e.name FROM EntityGroupEntity e " +
            "JOIN RelationEntity re ON re.fromId = e.id  " +
            "AND re.toId = :userId " +
            "AND re.toType = 'USER' " +
            "AND re.fromType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains'")
    Set<String> findUserGroupNamesByUserId(@Param("userId") UUID userId);

}
