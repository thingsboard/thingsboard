// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityGroupInfoEntity;

import java.util.List;
import java.util.UUID;

public interface EntityGroupInfoRepository extends JpaRepository<EntityGroupInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'ENTITY_GROUP', e.name) " +
            "FROM EntityGroupEntity e " +
            "WHERE e.id = :entityGroupId")
    EntityInfo findEntityGroupEntityInfoById(@Param("entityGroupId") UUID entityGroupId);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupInfoEntity> findEntityGroupsByType(@Param("parentEntityId") UUID parentEntityId,
                                                       @Param("parentEntityType") EntityType parentEntityType,
                                                       @Param("groupType") EntityType groupType,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'ENTITY_GROUP', e.name) " +
            "FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityInfo> findEntityGroupEntityInfosByType(@Param("parentEntityId") UUID parentEntityId,
                                                      @Param("parentEntityType") EntityType parentEntityType,
                                                      @Param("groupType") EntityType groupType,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId IN :ownerIds " +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupInfoEntity> findEntityGroupsByOwnerIdsAndType(@Param("ownerIds") List<UUID> ownerIds,
                                                                  @Param("groupType") EntityType groupType,
                                                                  @Param("textSearch") String textSearch,
                                                                  Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'ENTITY_GROUP', e.name) " +
            "FROM EntityGroupEntity e " +
            "WHERE e.ownerId IN :ownerIds " +
            "AND e.type = :groupType " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityInfo> findEntityGroupEntityInfosByOwnerIdsAndType(@Param("ownerIds") List<UUID> ownerIds,
                                                                 @Param("groupType") EntityType groupType,
                                                                 @Param("textSearch") String textSearch,
                                                                 Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.id IN :entityGroupIds " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupInfoEntity> findEntityGroupsByIds(@Param("entityGroupIds") List<UUID> entityGroupIds,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'ENTITY_GROUP', e.name) " +
            "FROM EntityGroupEntity e " +
            "WHERE e.id IN :entityGroupIds " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityInfo> findEntityGroupEntityInfosByIds(@Param("entityGroupIds") List<UUID> entityGroupIds,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE ((e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType) " +
            "OR (e.id IN :entityGroupIds)) " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupInfoEntity> findEntityGroupsByTypeOrIds(@Param("parentEntityId") UUID parentEntityId,
                                                            @Param("parentEntityType") EntityType parentEntityType,
                                                            @Param("groupType") EntityType groupType,
                                                            @Param("entityGroupIds") List<UUID> entityGroupIds,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(e.id, 'ENTITY_GROUP', e.name) " +
            "FROM EntityGroupEntity e " +
            "WHERE ((e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType) " +
            "OR (e.id IN :entityGroupIds)) " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityInfo> findEntityGroupEntityInfosByTypeOrIds(@Param("parentEntityId") UUID parentEntityId,
                                                           @Param("parentEntityType") EntityType parentEntityType,
                                                           @Param("groupType") EntityType groupType,
                                                           @Param("entityGroupIds") List<UUID> entityGroupIds,
                                                           @Param("textSearch") String textSearch,
                                                           Pageable pageable);

    @Query(value = "SELECT e.* FROM (select ev.*, ev.owner_ids as ownerids, ev.created_time as createdtime from entity_group_info_view ev) e, relation re " +
            "WHERE e.id = re.to_id AND re.to_type = 'ENTITY_GROUP' " +
            "AND re.relation_type_group = 'EDGE' " +
            "AND re.relation_type = :relationType " +
            "AND re.from_id = :edgeId AND re.from_type = 'EDGE' " +
            "AND exists (select 1 from json_array_elements(owner_ids) as owners " +
            "WHERE owners->>'id' = :ownerId " +
            "AND owners->>'entityType' = :ownerType) " +
            "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%'))",
            countQuery = "SELECT count(e.id) FROM entity_group_info_view e, relation re " +
                    "WHERE e.id = re.to_id AND re.to_type = 'ENTITY_GROUP' " +
                    "AND re.relation_type_group = 'EDGE' " +
                    "AND re.relation_type = :relationType " +
                    "AND re.from_id = :edgeId AND re.from_type = 'EDGE' " +
                    "AND exists (select 1 from json_array_elements(owner_ids) as owners " +
                    "WHERE owners->>'id' = :ownerId " +
                    "AND owners->>'entityType' = :ownerType) " +
                    "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%'))",
            nativeQuery = true)
    Page<EntityGroupInfoEntity> findEdgeEntityGroupsByOwnerIdAndType(@Param("edgeId") UUID edgeId,
                                                                     @Param("ownerId") String ownerId,
                                                                     @Param("ownerType") String ownerType,
                                                                     @Param("relationType") String relationType,
                                                                     @Param("textSearch") String textSearch,
                                                                     Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND e.name = :name")
    EntityGroupInfoEntity findEntityGroupByTypeAndName(@Param("parentEntityId") UUID parentEntityId,
                                                       @Param("parentEntityType") EntityType parentEntityType,
                                                       @Param("groupType") EntityType groupType,
                                                       @Param("name") String name);
}
