// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.mobile;

import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

public interface MobileAppDao extends Dao<MobileApp> {

    MobileApp findByBundleIdAndPlatformType(TenantId tenantId, MobileAppBundleId mobileAppBundleId, PlatformType platformType);

    PageData<MobileApp> findByTenantId(TenantId tenantId, PlatformType platformType, PageLink pageLink);

    void deleteByTenantId(TenantId tenantId);

    MobileApp findByPkgNameAndPlatformType(TenantId tenantId, String pkgName, PlatformType platform);
}
