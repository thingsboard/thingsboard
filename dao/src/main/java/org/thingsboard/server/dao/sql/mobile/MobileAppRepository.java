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
import org.thingsboard.server.dao.model.sql.MobileAppEntity;

import java.util.UUID;

public interface MobileAppRepository extends JpaRepository<MobileAppEntity, UUID> {

    @Query("SELECT a FROM MobileAppEntity a WHERE a.tenantId = :tenantId AND " +
            "(:platformType is NULL OR a.platformType = :platformType) AND" +
            "(:searchText is NULL OR ilike(a.pkgName, concat('%', :searchText, '%')) = true)")
    Page<MobileAppEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("platformType") PlatformType platformType,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    MobileAppEntity findByPkgNameAndPlatformType(@Param("pkgName") String pkgName, @Param("platformType") PlatformType platformType);

    @Transactional
    @Modifying
    @Query("DELETE FROM MobileAppEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM MobileAppEntity a LEFT JOIN MobileAppBundleEntity b ON b.androidAppId = a.id WHERE b.id = :bundleId")
    MobileAppEntity findAndroidAppByBundleId(@Param("bundleId") UUID bundleId);

    @Query("SELECT a FROM MobileAppEntity a LEFT JOIN MobileAppBundleEntity b ON b.iosAppID = a.id WHERE b.id = :bundleId")
    MobileAppEntity findIOSAppByBundleId(@Param("bundleId") UUID bundleId);

}
