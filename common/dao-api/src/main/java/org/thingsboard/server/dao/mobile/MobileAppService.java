// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.mobile;

import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface MobileAppService extends EntityDaoService {

    MobileApp saveMobileApp(TenantId tenantId, MobileApp mobileApp);

    MobileApp findMobileAppById(TenantId tenantId, MobileAppId mobileAppId);

    PageData<MobileApp> findMobileAppsByTenantId(TenantId tenantId, PlatformType platformType, PageLink pageLink);

    MobileApp findByBundleIdAndPlatformType(TenantId tenantId, MobileAppBundleId mobileAppBundleId, PlatformType platformType);

    MobileApp findMobileAppByPkgNameAndPlatformType(String pkgName, PlatformType platform);

    void deleteMobileAppById(TenantId tenantId, MobileAppId mobileAppId);

}
