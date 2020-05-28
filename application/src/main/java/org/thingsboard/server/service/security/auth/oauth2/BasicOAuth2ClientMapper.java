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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.dao.oauth2.OAuth2ClientMapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

@Service(value = "basicOAuth2ClientMapper")
@Slf4j
public class BasicOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {

    private static final String START_PLACEHOLDER_PREFIX = "%{";
    private static final String END_PLACEHOLDER_PREFIX = "}";
    private static final String EMAIL_TENANT_STRATEGY = "email";
    private static final String DOMAIN_TENANT_STRATEGY = "domain";
    private static final String CUSTOM_TENANT_STRATEGY = "custom";

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(OAuth2AuthenticationToken token, OAuth2ClientMapperConfig config) {
        OAuth2User oauth2User = new OAuth2User();
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String email = getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
        oauth2User.setEmail(email);
        oauth2User.setTenantName(getTenantName(attributes, config));
        if (!StringUtils.isEmpty(config.getBasic().getLastNameAttributeKey())) {
            String lastName = getStringAttributeByKey(attributes, config.getBasic().getLastNameAttributeKey());
            oauth2User.setLastName(lastName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getFirstNameAttributeKey())) {
            String firstName = getStringAttributeByKey(attributes, config.getBasic().getFirstNameAttributeKey());
            oauth2User.setFirstName(firstName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String customerName = sub.replace(config.getBasic().getCustomerNamePattern());
            oauth2User.setCustomerName(customerName);
        }

        return getOrCreateSecurityUserFromOAuth2User(oauth2User, config.isAllowUserCreation(), config.isActivateUser());
    }

    private String getTenantName(Map<String, Object> attributes, OAuth2ClientMapperConfig config) {
        switch (config.getBasic().getTenantNameStrategy()) {
            case EMAIL_TENANT_STRATEGY:
                return getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
            case DOMAIN_TENANT_STRATEGY:
                String email = getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
                return email.substring(email .indexOf("@") + 1);
            case CUSTOM_TENANT_STRATEGY:
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                return sub.replace(config.getBasic().getTenantNamePattern());
            default:
                throw new RuntimeException("Tenant Name Strategy with type " + config.getBasic().getTenantNameStrategy() + " is not supported!");
        }
    }

    private String getStringAttributeByKey(Map<String, Object> attributes, String key) {
        String result = null;
        try {
            result = (String) attributes.get(key);
        } catch (Exception e) {
            log.warn("Can't convert attribute to String by key " + key);
        }
        return result;
    }
}
