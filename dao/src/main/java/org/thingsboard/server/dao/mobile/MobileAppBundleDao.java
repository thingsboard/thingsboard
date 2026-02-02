/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.mobile;

import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface MobileAppBundleDao extends Dao<MobileAppBundle> {

    PageData<MobileAppBundleInfo> findInfosByTenantId(TenantId tenantId, PageLink pageLink);

    MobileAppBundleInfo findInfoById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    List<MobileAppBundleOauth2Client> findOauth2ClientsByMobileAppBundleId(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    void addOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client);

    void removeOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client);

    MobileAppBundle findByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform);

    void deleteByTenantId(TenantId tenantId);

}
