// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.mobile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.model.sql.MobileAppBundleEntity;
import org.thingsboard.server.dao.model.sql.MobileAppBundleInfoEntity;

import java.util.UUID;

public interface MobileAppBundleRepository extends JpaRepository<MobileAppBundleEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.MobileAppBundleInfoEntity(b, andApp.pkgName, iosApp.pkgName, " +
            "((andApp.status IS NOT NULL AND andApp.status = 'PUBLISHED') OR (iosApp.status IS NOT NULL AND iosApp.status = 'PUBLISHED'))) " +
            "FROM MobileAppBundleEntity b " +
            "LEFT JOIN MobileAppEntity andApp ON b.androidAppId = andApp.id " +
            "LEFT JOIN MobileAppEntity iosApp ON b.iosAppID = iosApp.id " +
            "WHERE b.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(b.title, concat('%', :searchText, '%')) = true)")
    Page<MobileAppBundleInfoEntity> findInfoByTenantId(@Param("tenantId") UUID tenantId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.MobileAppBundleInfoEntity(b, andApp.pkgName, iosApp.pkgName, " +
            "((andApp.status IS NOT NULL AND andApp.status = 'PUBLISHED') OR (iosApp.status IS NOT NULL AND iosApp.status = 'PUBLISHED'))) " +
            "FROM MobileAppBundleEntity b " +
            "LEFT JOIN MobileAppEntity andApp on b.androidAppId = andApp.id " +
            "LEFT JOIN MobileAppEntity iosApp on b.iosAppID = iosApp.id " +
            "WHERE b.id = :bundleId ")
    MobileAppBundleInfoEntity findInfoById(UUID bundleId);

    @Query("SELECT b " +
            "FROM MobileAppBundleEntity b " +
            "LEFT JOIN MobileAppEntity a ON b.androidAppId = a.id OR b.iosAppID = a.id " +
            "WHERE a.pkgName = :pkgName AND a.platformType = :platformType")
    MobileAppBundleEntity findByPkgNameAndPlatformType(@Param("pkgName") String pkgName,
                                                       @Param("platformType") PlatformType platformType);

    @Query("SELECT b FROM MobileAppBundleEntity b WHERE b.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(b.title, concat('%', :searchText, '%')) = true)")
    Page<MobileAppBundleEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM MobileAppBundleEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
