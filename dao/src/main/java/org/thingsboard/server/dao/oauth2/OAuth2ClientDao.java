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
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientDao extends Dao<OAuth2Client> {

    PageData<OAuth2Client> findByTenantId(UUID tenantId, PageLink pageLink);

    List<OAuth2Client> findEnabledByDomainName(String domainName);

    List<OAuth2Client> findEnabledByPkgNameAndPlatformType(String pkgName, PlatformType platformType);

    List<OAuth2Client> findByDomainId(UUID domainId);

    List<OAuth2Client> findByMobileAppBundleId(UUID mobileAppBundleId);

    String findAppSecret(UUID id, String pkgName, PlatformType platformType);

    void deleteByTenantId(UUID tenantId);

    List<OAuth2Client> findByIds(UUID tenantId, List<OAuth2ClientId> oAuth2ClientIds);

    boolean isPropagateToEdge(TenantId tenantId, UUID oAuth2ClientId);

}
