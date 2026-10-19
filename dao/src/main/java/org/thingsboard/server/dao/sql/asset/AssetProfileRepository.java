// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.edqs.fields.AssetProfileFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AssetProfileEntity;

import java.util.List;
import java.util.UUID;

public interface AssetProfileRepository extends JpaRepository<AssetProfileEntity, UUID>, ExportableEntityRepository<AssetProfileEntity> {

    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a " +
            "WHERE a.id = :assetProfileId")
    AssetProfileInfo findAssetProfileInfoById(@Param("assetProfileId") UUID assetProfileId);

    @Query("SELECT a FROM AssetProfileEntity a WHERE " +
            "a.tenantId = :tenantId AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetProfileEntity> findAssetProfiles(@Param("tenantId") UUID tenantId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE " +
            "a.tenantId = :tenantId AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AssetProfileInfo> findAssetProfileInfos(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT a FROM AssetProfileEntity a " +
            "WHERE a.tenantId = :tenantId AND a.isDefault = true")
    AssetProfileEntity findByDefaultTrueAndTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a " +
            "WHERE a.tenantId = :tenantId AND a.isDefault = true")
    AssetProfileInfo findDefaultAssetProfileInfo(@Param("tenantId") UUID tenantId);

    AssetProfileEntity findByTenantIdAndName(UUID id, String profileName);

    @Query("SELECT externalId FROM AssetProfileEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE a.tenantId = :tenantId AND a.image = :imageLink")
    List<AssetProfileInfo> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, Pageable page);

    @Query("SELECT new org.thingsboard.server.common.data.asset.AssetProfileInfo(a.id, a.tenantId, a.name, a.image, a.defaultDashboardId) " +
            "FROM AssetProfileEntity a WHERE a.image = :imageLink")
    List<AssetProfileInfo> findByImageLink(@Param("imageLink") String imageLink, Pageable page);

    Page<AssetProfileEntity> findAllByImageNotNull(Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(ap.id, 'ASSET_PROFILE', ap.name) " +
            "FROM AssetProfileEntity ap WHERE ap.tenantId = :tenantId AND EXISTS " +
            "(SELECT 1 FROM AssetEntity a WHERE a.tenantId = :tenantId AND a.assetProfileId = ap.id)")
    List<EntityInfo> findActiveTenantAssetProfileNames(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'ASSET_PROFILE', a.name) " +
            "FROM AssetProfileEntity a WHERE a.tenantId = :tenantId")
    List<EntityInfo> findAllTenantAssetProfileNames(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.AssetProfileFields(a.id, a.createdTime, a.tenantId," +
            "a.name, a.version, a.isDefault) FROM AssetProfileEntity a WHERE a.id > :id ORDER BY a.id")
    List<AssetProfileFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
