// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientService extends EntityDaoService {

    List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByDomainName(String domainName);

    List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(String pkgName, PlatformType platformType);

    List<OAuth2Client> findOAuth2ClientsByTenantId(TenantId tenantId);

    OAuth2Client saveOAuth2Client(TenantId tenantId, OAuth2Client oAuth2Client);

    OAuth2Client findOAuth2ClientById(TenantId tenantId, OAuth2ClientId providerId);

    String findAppSecret(OAuth2ClientId oAuth2ClientId, String pkgName, PlatformType platformType);

    void deleteOAuth2ClientById(TenantId tenantId, OAuth2ClientId oAuth2ClientId);

    void deleteOauth2ClientsByTenantId(TenantId tenantId);

    PageData<OAuth2ClientInfo> findOAuth2ClientInfosByTenantId(TenantId tenantId, PageLink pageLink);

    List<OAuth2ClientInfo> findOAuth2ClientInfosByIds(TenantId tenantId, List<OAuth2ClientId> oAuth2ClientIds);

    boolean isPropagateOAuth2ClientToEdge(TenantId tenantId, OAuth2ClientId oAuth2ClientId);

}
