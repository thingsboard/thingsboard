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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String OAUTH2_CLIENT_REGISTRATIONS_PARAMS = "oauth2ClientRegistrationsParams";

    private static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    @Autowired
    private AdminSettingsService adminSettingsService;

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
        // TODO check by registration ID in entities
        AdminSettings clientRegistrationParamsSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
        if (clientRegistrationParamsSettings == null) {
            clientRegistrationParamsSettings = new AdminSettings();
            clientRegistrationParamsSettings.setKey(OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
            ObjectNode node = mapper.createObjectNode();
            clientRegistrationParamsSettings.setJsonValue(node);
        }
        String json;
        try {
            json = mapper.writeValueAsString(clientRegistration);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert OAuth2 Client Registration Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert OAuth2 Client Registration Params to JSON!");
        }
        ObjectNode oldClientRegistrations = (ObjectNode) clientRegistrationParamsSettings.getJsonValue();
        oldClientRegistrations.put(clientRegistration.getRegistrationId(), json);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, clientRegistrationParamsSettings);
        // TODO ask if that's worth it
        return getClientRegistration(clientRegistration.getRegistrationId());
    }

    @Override
    public OAuth2ClientRegistration saveTenantOAuth2ClientRegistration(TenantId tenantId, OAuth2ClientRegistration clientRegistration) {
        // TODO check by registration ID in system
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
