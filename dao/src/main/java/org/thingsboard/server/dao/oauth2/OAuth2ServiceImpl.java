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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    @Autowired(required = false)
    OAuth2Configuration oauth2Configuration;

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients() {
        return Collections.emptyList();
    }

    @Override
    public List<OAuth2ClientRegistration> getSystemOAuth2ClientRegistrations(TenantId tenantId) {
        return null;
    }

    @Override
    public List<OAuth2ClientRegistration> getTenantOAuth2ClientRegistrations(TenantId tenantId) {
        return null;
    }

    @Override
    public List<OAuth2ClientRegistration> getCustomerOAuth2ClientRegistrations(TenantId tenantId, CustomerId customerId) {
        return null;
    }

    @Override
    public OAuth2ClientRegistration saveSystemOAuth2ClientRegistration(OAuth2ClientRegistration clientRegistration) {
        return null;
    }

    @Override
    public OAuth2ClientRegistration saveTenantOAuth2ClientRegistration(TenantId tenantId, OAuth2ClientRegistration clientRegistration) {
        return null;
    }

    @Override
    public OAuth2ClientRegistration saveCustomerOAuth2ClientRegistration(TenantId tenantId, CustomerId customerId, OAuth2ClientRegistration clientRegistration) {
        return null;
    }

    @Override
    public void deleteDomainOAuth2ClientRegistrationByEntityId(TenantId tenantId, EntityId entityId) {

    }

    @Override
    public boolean isOAuth2ClientRegistrationAllowed(TenantId tenantId, EntityId entityId) {
        return false;
    }

    @Override
    public boolean isCustomerOAuth2ClientRegistrationAllowed(TenantId tenantId) {
        return false;
    }

    @Override
    public OAuth2ClientRegistration getClientRegistration(String registrationId) {
        return null;
    }
}
