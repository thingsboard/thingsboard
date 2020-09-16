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

import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OAuth2Utils {
    public static final String ALLOW_OAUTH2_CONFIGURATION = "allowOAuth2Configuration";
    public static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    public static OAuth2ClientInfo toClientInfo(OAuth2ClientRegistration clientRegistration) {
        OAuth2ClientInfo client = new OAuth2ClientInfo();
        client.setName(clientRegistration.getLoginButtonLabel());
        client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, clientRegistration.getUuidId().toString()));
        client.setIcon(clientRegistration.getLoginButtonIcon());
        return client;
    }

    public static List<OAuth2ClientRegistration> toClientRegistrations(TenantId tenantId, OAuth2ClientsParams clientsParams) {
        return clientsParams.getOAuth2DomainDtos().stream()
                .flatMap(domainParams -> domainParams.getClientRegistrations().stream()
                        .map(clientRegistrationDto -> OAuth2Utils.toClientRegistration(tenantId, domainParams.getDomainName(),
                                domainParams.getRedirectUriTemplate(), clientRegistrationDto)
                        ))
                .collect(Collectors.toList());
    }

    public static OAuth2ClientsParams toOAuth2ClientsParams(List<OAuth2ClientRegistration> clientRegistrations) {
        Map<String, OAuth2ClientsDomainParams> domainParamsMap = new HashMap<>();
        for (OAuth2ClientRegistration clientRegistration : clientRegistrations) {
            String domainName = clientRegistration.getDomainName();
            OAuth2ClientsDomainParams domainParams = domainParamsMap.computeIfAbsent(domainName,
                    key -> new OAuth2ClientsDomainParams(domainName, clientRegistration.getRedirectUriTemplate(), new ArrayList<>())
            );
            domainParams.getClientRegistrations()
                    .add(toClientRegistrationDto(clientRegistration));
        }
        return new OAuth2ClientsParams(new ArrayList<>(domainParamsMap.values()));
    }

    public static ClientRegistrationDto toClientRegistrationDto(OAuth2ClientRegistration oAuth2ClientRegistration) {
        return ClientRegistrationDto.builder()
                .id(oAuth2ClientRegistration.getId())
                .createdTime(oAuth2ClientRegistration.getCreatedTime())
                .mapperConfig(oAuth2ClientRegistration.getMapperConfig())
                .clientId(oAuth2ClientRegistration.getClientId())
                .clientSecret(oAuth2ClientRegistration.getClientSecret())
                .authorizationUri(oAuth2ClientRegistration.getAuthorizationUri())
                .accessTokenUri(oAuth2ClientRegistration.getAccessTokenUri())
                .scope(oAuth2ClientRegistration.getScope())
                .userInfoUri(oAuth2ClientRegistration.getUserInfoUri())
                .userNameAttributeName(oAuth2ClientRegistration.getUserNameAttributeName())
                .jwkSetUri(oAuth2ClientRegistration.getJwkSetUri())
                .clientAuthenticationMethod(oAuth2ClientRegistration.getClientAuthenticationMethod())
                .loginButtonLabel(oAuth2ClientRegistration.getLoginButtonLabel())
                .loginButtonIcon(oAuth2ClientRegistration.getLoginButtonIcon())
                .build();
    }

    public static OAuth2ClientRegistration toClientRegistration(TenantId tenantId, String domainName, String redirectUriTemplate,
                                                                 ClientRegistrationDto clientRegistrationDto) {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setId(clientRegistrationDto.getId());
        clientRegistration.setTenantId(tenantId);
        clientRegistration.setCreatedTime(clientRegistrationDto.getCreatedTime());
        clientRegistration.setDomainName(domainName);
        clientRegistration.setRedirectUriTemplate(redirectUriTemplate);
        clientRegistration.setMapperConfig(clientRegistrationDto.getMapperConfig());
        clientRegistration.setClientId(clientRegistrationDto.getClientId());
        clientRegistration.setClientSecret(clientRegistrationDto.getClientSecret());
        clientRegistration.setAuthorizationUri(clientRegistrationDto.getAuthorizationUri());
        clientRegistration.setAccessTokenUri(clientRegistrationDto.getAccessTokenUri());
        clientRegistration.setScope(clientRegistrationDto.getScope());
        clientRegistration.setUserInfoUri(clientRegistrationDto.getUserInfoUri());
        clientRegistration.setUserNameAttributeName(clientRegistrationDto.getUserNameAttributeName());
        clientRegistration.setJwkSetUri(clientRegistrationDto.getJwkSetUri());
        clientRegistration.setClientAuthenticationMethod(clientRegistrationDto.getClientAuthenticationMethod());
        clientRegistration.setLoginButtonLabel(clientRegistrationDto.getLoginButtonLabel());
        clientRegistration.setLoginButtonIcon(clientRegistrationDto.getLoginButtonIcon());
        return clientRegistration;
    }
}
