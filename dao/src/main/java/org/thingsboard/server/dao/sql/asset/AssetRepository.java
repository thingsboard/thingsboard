// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.AssetFields;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AssetEntity;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, ExportableEntityRepository<AssetEntity> {

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
           "  OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("textSearch") String textSearch,
                                     Pageable pageable);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
           "AND a.customerId = :customerId " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
           "  OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                  @Param("customerId") UUID customerId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
           "AND a.assetProfileId = :profileId " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                 @Param("profileId") UUID profileId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT a FROM AssetEntity a, " +
           "RelationEntity re " +
           "WHERE a.id = re.toId AND re.toType = 'ASSET' " +
           "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
           "AND re.relationType = 'Contains' " +
           "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT a FROM AssetEntity a, " +
           "RelationEntity re " +
           "WHERE a.id = re.toId AND re.toType = 'ASSET' " +
           "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
           "AND re.relationType = 'Contains' " +
           "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT a.id FROM AssetEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND a.assetProfileId = :assetProfileId " +
           "AND (:textSearch IS NULL OR ilike(a.type, CONCAT('%', :textSearch, '%')) = true) ")
    Page<UUID> findAssetIdsByTenantIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                       @Param("assetProfileId") UUID assetProfileId,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query("SELECT a FROM AssetEntity a, " +
           "RelationEntity re " +
           "WHERE a.id = re.toId AND re.toType = 'ASSET' " +
           "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
           "AND re.relationType = 'Contains' " +
           "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
           "AND a.type = :type " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                  @Param("type") String type,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    List<AssetEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> assetIds);

    List<AssetEntity> findByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> assetIds);

    // First-match (not strict single-result): under Citus the (tenant_id, name) UNIQUE constraint is dropped,
    // so a select-then-insert race can momentarily leave duplicate rows. Returning the first match converts the
    // resulting IncorrectResultSizeDataAccessException (HTTP 500) into the intended DataValidationException (400);
    // it does not fix the inherent race (best-effort). Plain mode keeps the constraint, so at most one row matches.
    AssetEntity findFirstByTenantIdAndName(UUID tenantId, String name);

    @Override
    @Query(value = "SELECT * FROM asset WHERE tenant_id = :tenantId AND external_id = :externalId LIMIT 1", nativeQuery = true)
    AssetEntity findByTenantIdAndExternalId(@Param("tenantId") UUID tenantId, @Param("externalId") UUID externalId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ASSET', a.name) " +
            "FROM AssetEntity a WHERE a.tenantId = :tenantId AND a.name LIKE CONCAT(:prefix, '%')")
    List<EntityInfo> findEntityInfosByNamePrefix(UUID tenantId, String prefix);

    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
           "AND a.type = :type " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "  OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                            @Param("type") String type,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);


    @Query("SELECT a FROM AssetEntity a WHERE a.tenantId = :tenantId " +
           "AND a.customerId = :customerId AND a.type = :type " +
           "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
           "OR ilike(a.label, CONCAT('%', :textSearch, '%')) = true) ")
    Page<AssetEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                         @Param("customerId") UUID customerId,
                                                         @Param("type") String type,
                                                         @Param("textSearch") String textSearch,
                                                         Pageable pageable);

    Long countByAssetProfileId(UUID assetProfileId);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT a.id FROM AssetEntity a WHERE a.tenantId = :tenantId AND (a.customerId is null OR a.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a.id FROM AssetEntity a WHERE a.tenantId = :tenantId AND a.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    @Query("SELECT externalId FROM AssetEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query(value = "SELECT DISTINCT new org.thingsboard.server.common.data.util.TbPair(a.tenantId , a.type) FROM  AssetEntity a")
    Page<TbPair<UUID, String>> getAllAssetTypes(Pageable pageable);


    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.AssetFields(a.id, a.createdTime, a.tenantId, a.customerId," +
           "a.name, a.version, a.type, a.label, a.assetProfileId, a.additionalInfo) FROM AssetEntity a WHERE a.id > :id ORDER BY a.id")
    List<AssetFields> findAllFields(@Param("id") UUID id, Limit limit);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ASSET', a.name) " +
           "FROM AssetEntity a WHERE a.tenantId = :tenantId AND a.assetProfileId = :profileId")
    Page<EntityInfo> findEntityInfosByTenantIdAndProfileId(UUID tenantId, UUID profileId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ASSET', a.name) " +
           "FROM AssetEntity a WHERE a.id = :id")
    EntityInfo findEntityInfoById(UUID id);

    @Query("SELECT DISTINCT new org.thingsboard.server.common.data.AssetCacheInfo(a.id, a.tenantId, a.customerId, a.name, a.type, a.assetProfileId) " +
           "FROM AssetEntity a, RelationEntity re WHERE a.id = re.toId AND re.toType = 'ASSET' " +
           "AND re.relationType = :relationType AND a.id > :id ORDER BY a.id")
    List<AssetCacheInfo> findAssetCacheInfosByRelationType(@Param("relationType") String relationType,
                                                           @Param("id") UUID id,
                                                           Limit limit);

}
