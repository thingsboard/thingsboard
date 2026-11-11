// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface MobileAppBundleService extends EntityDaoService {

    MobileAppBundle saveMobileAppBundle(TenantId tenantId, MobileAppBundle mobileAppBundle);

    void updateOauth2Clients(TenantId tenantId, MobileAppBundleId mobileAppBundleId, List<OAuth2ClientId> oAuth2ClientIds);

    MobileAppBundle findMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    PageData<MobileAppBundleInfo> findMobileAppBundleInfosByTenantId(TenantId tenantId, PageLink pageLink);

    MobileAppBundleInfo findMobileAppBundleInfoById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

    MobileAppBundle findMobileAppBundleByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform, boolean fetchPolicyInfo);

    MobileSelfRegistrationParams getMobileSelfRegistrationParams(TenantId tenantId, String pkgName, PlatformType platformType);

    String getMobilePrivacyPolicy(TenantId tenantId, String pkgName, PlatformType platformType);

    String getMobileTermsOfUse(TenantId tenantId, String pkgName, PlatformType platformType);

    void deleteMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId);

}
