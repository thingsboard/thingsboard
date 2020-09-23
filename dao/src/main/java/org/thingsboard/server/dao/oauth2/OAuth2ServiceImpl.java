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
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
public class OAuth2ServiceImpl extends AbstractEntityService implements OAuth2Service {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CLIENT_REGISTRATION_ID = "Incorrect clientRegistrationId ";
    public static final String INCORRECT_DOMAIN_NAME = "Incorrect domainName ";

    @Autowired
    private OAuth2ClientRegistrationDao clientRegistrationDao;

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients(String domainName) {
        log.trace("Executing getOAuth2Clients [{}]", domainName);
        validateString(domainName, INCORRECT_DOMAIN_NAME + domainName);
        return clientRegistrationDao.findByDomainName(domainName).stream()
                .filter(OAuth2ClientRegistration::isEnabled)
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OAuth2ClientsParams saveOAuth2Params(OAuth2ClientsParams oauth2Params) {
        log.trace("Executing saveOAuth2Params [{}]", oauth2Params);
        clientParamsValidator.accept(oauth2Params);
        List<OAuth2ClientRegistration> inputClientRegistrations = OAuth2Utils.toClientRegistrations(oauth2Params);
        List<OAuth2ClientRegistration> savedClientRegistrations = inputClientRegistrations.stream()
                .map(clientRegistration -> clientRegistrationDao.save(TenantId.SYS_TENANT_ID, clientRegistration))
                .collect(Collectors.toList());
        return OAuth2Utils.toOAuth2Params(savedClientRegistrations);
    }

    @Override
    public OAuth2ClientsParams findOAuth2Params() {
        log.trace("Executing findOAuth2Params");
        return OAuth2Utils.toOAuth2Params(clientRegistrationDao.findAll());
    }

    @Override
    public OAuth2ClientRegistration findClientRegistration(UUID id) {
        log.trace("Executing findClientRegistration [{}]", id);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        return clientRegistrationDao.findById(null, id);
    }

    @Override
    public List<OAuth2ClientRegistration> findAllClientRegistrations() {
        log.trace("Executing findAllClientRegistrations");
        return clientRegistrationDao.findAll();
    }

    @Override
    public void deleteClientRegistrationById(OAuth2ClientRegistrationId id) {
        log.trace("Executing deleteClientRegistrationById [{}]", id);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        clientRegistrationDao.removeById(TenantId.SYS_TENANT_ID, id.getId());
    }

    @Override
    @Transactional
    public void deleteClientRegistrationsByDomain(String domain) {
        log.trace("Executing deleteClientRegistrationsByDomain [{}]", domain);
        validateString(domain, INCORRECT_DOMAIN_NAME + domain);
        clientRegistrationDao.removeByDomainName(domain);
    }

    private final Consumer<OAuth2ClientsParams> clientParamsValidator = oauth2Params -> {
        if (oauth2Params == null
                || oauth2Params.getOAuth2DomainDtos() == null
                || oauth2Params.getOAuth2DomainDtos().isEmpty()) {
            throw new DataValidationException("Domain params should be specified!");
        }
        for (OAuth2ClientsDomainParams domainParams : oauth2Params.getOAuth2DomainDtos()) {
            if (StringUtils.isEmpty(domainParams.getDomainName())) {
                throw new DataValidationException("Domain name should be specified!");
            }
            if (StringUtils.isEmpty(domainParams.getRedirectUriTemplate())) {
                throw new DataValidationException("Redirect URI template should be specified!");
            }
            if (domainParams.getClientRegistrations() == null || domainParams.getClientRegistrations().isEmpty()) {
                throw new DataValidationException("Client registrations should be specified!");
            }
            for (ClientRegistrationDto clientRegistration : domainParams.getClientRegistrations()) {
                if (StringUtils.isEmpty(clientRegistration.getClientId())) {
                    throw new DataValidationException("Client ID should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
                    throw new DataValidationException("Client secret should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
                    throw new DataValidationException("Authorization uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAccessTokenUri())) {
                    throw new DataValidationException("Token uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getScope())) {
                    throw new DataValidationException("Scope should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserInfoUri())) {
                    throw new DataValidationException("User info uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
                    throw new DataValidationException("User name attribute name should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
                    throw new DataValidationException("Client authentication method should be specified!");
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
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
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
                    OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
                    if (customConfig == null) {
                        throw new DataValidationException("Custom config should be specified!");
                    }
                    if (StringUtils.isEmpty(customConfig.getUrl())) {
                        throw new DataValidationException("Custom mapper URL should be specified!");
                    }
                }
            }
        }
    };
}
