/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientService extends EntityDaoService {

    List<OAuth2ClientInfo> getWebOAuth2Clients(String domainName, PlatformType platformType);

    List<OAuth2ClientInfo> getMobileOAuth2Clients(String pkgName, PlatformType platformType);

    List<OAuth2RegistrationInfo> findOauth2ClientInfosByTenantId(TenantId tenantId);

    List<OAuth2Registration> findOauth2ClientsByTenantId(TenantId tenantId);

    OAuth2Registration saveOAuth2Client(TenantId tenantId, OAuth2Registration oAuth2Registration);

    OAuth2Registration findOAuth2ClientById(TenantId tenantId, OAuth2RegistrationId providerId);

    String findAppSecret(UUID registrationId, String pkgName);

    void deleteById(TenantId tenantId, OAuth2RegistrationId oAuth2RegistrationId);
}
