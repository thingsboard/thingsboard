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
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface MobileAppBundleService extends EntityDaoService {

    MobileAppBundle saveMobileAppBundle(TenantId tenantId, MobileAppBundle mobileAppBundle);

    void updateOauth2Clients(TenantId tenantId, MobileAppBundleId mobileAppBundleId, List<OAuth2ClientId> oAuth2ClientIds);

    MobileAppBundle findMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    PageData<MobileAppBundleInfo> findMobileAppBundleInfosByTenantId(TenantId tenantId, PageLink pageLink);

    MobileAppBundleInfo findMobileAppBundleInfoById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    MobileAppBundle findMobileAppBundleByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform);

    void deleteMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

}
