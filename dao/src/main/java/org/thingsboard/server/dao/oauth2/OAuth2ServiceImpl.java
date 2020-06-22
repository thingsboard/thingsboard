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
        if (oauth2Configuration == null || !oauth2Configuration.isEnabled()) {
            return Collections.emptyList();
        }

        Stream<OAuth2ClientInfo> startUpConfiguration = oauth2Configuration.getClients().entrySet().stream()
                .map(entry -> {
                    OAuth2ClientInfo client = new OAuth2ClientInfo();
                    client.setName(entry.getValue().getLoginButtonLabel());
                    client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, entry.getKey()));
                    client.setIcon(entry.getValue().getLoginButtonIcon());
                    return client;
                });

        return startUpConfiguration.collect(Collectors.toList());
    }

    @Override
    public OAuth2ClientRegistration getClientRegistrationByRegistrationId(String registrationId) {
        if (oauth2Configuration == null || !oauth2Configuration.isEnabled()) return null;
        OAuth2Client oAuth2Client = oauth2Configuration.getClients() == null ? null : oauth2Configuration.getClients().get(registrationId);
        if (oAuth2Client != null){
            return toClientRegistration(registrationId, oAuth2Client);
        } else {
            return null;
        }
    }

    private OAuth2ClientRegistration toClientRegistration(String registrationId, OAuth2Client oAuth2Client) {
        OAuth2ClientMapperConfig mapperConfig = oAuth2Client.getMapperConfig();
        OAuth2ClientMapperConfig.BasicOAuth2ClientMapperConfig basicConfig = mapperConfig.getBasic();
        OAuth2ClientMapperConfig.CustomOAuth2ClientMapperConfig customConfig = mapperConfig.getCustom();

        return OAuth2ClientRegistration.builder()
                .registrationId(registrationId)
                .mapperConfig(OAuth2MapperConfig.builder()
                        .allowUserCreation(mapperConfig.isAllowUserCreation())
                        .activateUser(mapperConfig.isActivateUser())
                        .type(MapperType.valueOf(
                                mapperConfig.getType().toUpperCase()
                        ))
                        .basicConfig(
                                OAuth2BasicMapperConfig.builder()
                                        .emailAttributeKey(basicConfig.getEmailAttributeKey())
                                        .firstNameAttributeKey(basicConfig.getFirstNameAttributeKey())
                                        .lastNameAttributeKey(basicConfig.getLastNameAttributeKey())
                                        .tenantNameStrategy(TenantNameStrategyType.valueOf(
                                                basicConfig.getTenantNameStrategy().toUpperCase()
                                        ))
                                        .tenantNamePattern(basicConfig.getTenantNamePattern())
                                        .customerNamePattern(basicConfig.getCustomerNamePattern())
                                        .defaultDashboardName(basicConfig.getDefaultDashboardName())
                                        .alwaysFullScreen(basicConfig.isAlwaysFullScreen())
                                        .build()
                        )
                        .customConfig(
                                OAuth2CustomMapperConfig.builder()
                                        .url(customConfig.getUrl())
                                        .username(customConfig.getUsername())
                                        .password(customConfig.getPassword())
                                        .build()
                        )
                        .build())
                .clientId(oAuth2Client.getClientId())
                .clientSecret(oAuth2Client.getClientSecret())
                .authorizationUri(oAuth2Client.getAuthorizationUri())
                .tokenUri(oAuth2Client.getAccessTokenUri())
                .redirectUriTemplate(oAuth2Client.getRedirectUriTemplate())
                .scope(oAuth2Client.getScope())
                .authorizationGrantType(oAuth2Client.getAuthorizationGrantType())
                .userInfoUri(oAuth2Client.getUserInfoUri())
                .userNameAttributeName(oAuth2Client.getUserNameAttributeName())
                .jwkSetUri(oAuth2Client.getJwkSetUri())
                .clientAuthenticationMethod(oAuth2Client.getClientAuthenticationMethod())
                .clientName(oAuth2Client.getClientName())
                .loginButtonLabel(oAuth2Client.getLoginButtonLabel())
                .loginButtonIcon(oAuth2Client.getLoginButtonIcon())
                .build();
    }
}
