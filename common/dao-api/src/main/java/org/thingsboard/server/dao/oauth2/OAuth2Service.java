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

import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientsParams;

import java.util.List;

public interface OAuth2Service {
    Pair<TenantId, OAuth2ClientRegistration> getClientRegistrationWithTenant(String registrationId);

    OAuth2ClientRegistration getClientRegistration(String registrationId);

    List<OAuth2ClientInfo> getOAuth2Clients(String domainName);

    OAuth2ClientsParams saveSystemOAuth2ClientsParams(OAuth2ClientsParams oAuth2ClientsParams);

    OAuth2ClientsParams saveTenantOAuth2ClientsParams(TenantId tenantId, OAuth2ClientsParams oAuth2ClientsParams);

    OAuth2ClientsParams getSystemOAuth2ClientsParams();

    OAuth2ClientsParams getTenantOAuth2ClientsParams(TenantId tenantId);

    void deleteTenantOAuth2ClientsParams(TenantId tenantId);

    void deleteSystemOAuth2ClientsParams();

    boolean isOAuth2ClientRegistrationAllowed(TenantId tenantId);

}
