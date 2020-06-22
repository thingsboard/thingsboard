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
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;
import java.util.function.Consumer;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
public class OAuth2ClientRegistrationServiceImpl implements OAuth2ClientRegistrationService {
    public static final String INCORRECT_REGISTRATION_ID = "Incorrect registrationId ";

    @Autowired
    private OAuth2ClientRegistrationDao clientRegistrationDao;

    @Override
    public OAuth2ClientRegistration saveClientRegistration(OAuth2ClientRegistration clientRegistration) {
        log.trace("Executing saveClientRegistration [{}]", clientRegistration);
        // TODO add checking for duplicates and other validations
        return clientRegistrationDao.save(clientRegistration);
    }

    @Override
    public List<OAuth2ClientRegistration> findClientRegistrations() {
        log.trace("Executing findClientRegistrations []");
        return clientRegistrationDao.find();
    }

    @Override
    public OAuth2ClientRegistration findClientRegistrationsByRegistrationId(String registrationId) {
        log.trace("Executing findClientRegistrationsByRegistrationId [{}]", registrationId);
        validateString(registrationId, INCORRECT_REGISTRATION_ID + registrationId);
        return clientRegistrationDao.findByRegistrationId(registrationId);
    }

    @Override
    public void deleteClientRegistrationsByRegistrationId(String registrationId) {
        log.trace("Executing deleteClientRegistrationsByRegistrationId [{}]", registrationId);
        validateString(registrationId, INCORRECT_REGISTRATION_ID + registrationId);
        clientRegistrationDao.removeByRegistrationId(registrationId);
    }

    private Consumer<OAuth2ClientRegistration> validator = clientRegistration -> {
        if (StringUtils.isEmpty(clientRegistration.getRegistrationId())) {
            throw new DataValidationException("Registration ID should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientId())) {
            throw new DataValidationException("Client ID should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
            throw new DataValidationException("Client secret should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
            throw new DataValidationException("Authorization uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getTokenUri())) {
            throw new DataValidationException("Token uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getRedirectUriTemplate())) {
            throw new DataValidationException("Redirect uri template should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getScope())) {
            throw new DataValidationException("Scope should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getAuthorizationGrantType())) {
            throw new DataValidationException("Authorization grant type should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getUserInfoUri())) {
            throw new DataValidationException("User info uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
            throw new DataValidationException("User name attribute name should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getJwkSetUri())) {
            throw new DataValidationException("Jwk set uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
            throw new DataValidationException("Client authentication method should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientName())) {
            throw new DataValidationException("Client name should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getLoginButtonLabel())) {
            throw new DataValidationException("Login button label should be specified!");
        }
        OAuth2MapperConfig mapperConfig = clientRegistration.getMapperConfig();
        if (mapperConfig == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (mapperConfig.getType() == null) {
            throw new DataValidationException("Mapper config type should be specified!");
        }
        if (mapperConfig.getType() == MapperType.BASIC) {
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasicConfig();
            if (basicConfig == null) {
                throw new DataValidationException("Basic config should be specified!");
            }
            if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                throw new DataValidationException("Email attribute key should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == null) {
                throw new DataValidationException("Tenant name strategy should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                    && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                throw new DataValidationException("Tenant name pattern should be specified!");
            }
        }
        if (mapperConfig.getType() == MapperType.CUSTOM) {
            OAuth2CustomMapperConfig customConfig = mapperConfig.getCustomConfig();
            if (customConfig == null) {
                throw new DataValidationException("Custom config should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getUrl())) {
                throw new DataValidationException("Custom mapper URL should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getUsername())) {
                throw new DataValidationException("Custom mapper username should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getPassword())) {
                throw new DataValidationException("Custom mapper password should be specified!");
            }
        }
    };
}
