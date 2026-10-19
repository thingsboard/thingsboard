// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.mobile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.model.sql.MobileAppBundlePolicyInfoEntity;

import java.util.UUID;


public interface MobileAppBundlePolicyInfoRepository extends JpaRepository<MobileAppBundlePolicyInfoEntity, UUID> {

    @Query("SELECT b " +
            "FROM MobileAppBundlePolicyInfoEntity b " +
            "LEFT JOIN MobileAppEntity a on b.androidAppId = a.id or b.iosAppID = a.id " +
            "WHERE a.pkgName = :pkgName AND a.platformType = :platformType")
    MobileAppBundlePolicyInfoEntity findByPkgNameAndPlatformType(@Param("pkgName") String pkgName,
                                                                 @Param("platformType") PlatformType platformType);


}
