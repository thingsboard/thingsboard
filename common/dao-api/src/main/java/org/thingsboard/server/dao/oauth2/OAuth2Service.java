/**
 * Copyright © 2016-2020 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;

import java.util.List;

public interface OAuth2Service {
    OAuth2ClientRegistration getClientRegistration(String registrationId);

    List<OAuth2ClientInfo> getOAuth2Clients();

    List<OAuth2ClientRegistration> getSystemOAuth2ClientRegistrations(TenantId tenantId);

    List<OAuth2ClientRegistration> getTenantOAuth2ClientRegistrations(TenantId tenantId);

    List<OAuth2ClientRegistration> getCustomerOAuth2ClientRegistrations(TenantId tenantId, CustomerId customerId);

    OAuth2ClientRegistration saveSystemOAuth2ClientRegistration(OAuth2ClientRegistration clientRegistration);

    OAuth2ClientRegistration saveTenantOAuth2ClientRegistration(TenantId tenantId, OAuth2ClientRegistration clientRegistration);

    OAuth2ClientRegistration saveCustomerOAuth2ClientRegistration(TenantId tenantId, CustomerId customerId, OAuth2ClientRegistration clientRegistration);

    void deleteDomainOAuth2ClientRegistrationByEntityId(TenantId tenantId, EntityId entityId);

    boolean isOAuth2ClientRegistrationAllowed(TenantId tenantId, EntityId entityId);

    boolean isCustomerOAuth2ClientRegistrationAllowed(TenantId tenantId);


}
