/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;

import java.util.List;
import java.util.UUID;

public interface OAuth2Service {
    List<OAuth2ClientInfo> getOAuth2Clients(String domainName);

    OAuth2ClientRegistration saveClientRegistration(OAuth2ClientRegistration clientRegistration);

    List<OAuth2ClientRegistration> findClientRegistrationsByTenantId(TenantId tenantId);

    OAuth2ClientRegistration findClientRegistration(UUID id);

    List<OAuth2ClientRegistration> findAllClientRegistrations();

    void deleteClientRegistrationsByTenantId(TenantId tenantId);

    void deleteClientRegistrationById(TenantId tenantId, OAuth2ClientRegistrationId id);

    boolean isOAuth2ClientRegistrationAllowed(TenantId tenantId);
}
