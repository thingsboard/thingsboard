/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.deprecated.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.common.data.oauth2.deprecated.ClientRegistrationDto;
import org.thingsboard.server.common.data.oauth2.deprecated.DomainInfo;
import org.thingsboard.server.common.data.oauth2.deprecated.ExtendedOAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientRegistration;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientsDomainParams;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientsParams;

import java.util.*;
import java.util.stream.Collectors;

public class OAuth2Utils {
    public static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    public static OAuth2ClientInfo toClientInfo(OAuth2Registration registration) {
        OAuth2ClientInfo client = new OAuth2ClientInfo();
        client.setName(registration.getLoginButtonLabel());
        client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, registration.getUuidId().toString()));
        client.setIcon(registration.getLoginButtonIcon());
        return client;
    }

    public static OAuth2ClientsParams toOAuth2Params(List<ExtendedOAuth2ClientRegistrationInfo> extendedOAuth2ClientRegistrationInfos) {
        Map<OAuth2ClientRegistrationInfoId, List<DomainInfo>> domainsByInfoId = new LinkedHashMap<>();
        Map<OAuth2ClientRegistrationInfoId, OAuth2ClientRegistrationInfo> infoById = new LinkedHashMap<>();
        for (ExtendedOAuth2ClientRegistrationInfo extendedClientRegistrationInfo : extendedOAuth2ClientRegistrationInfos) {
            String domainName = extendedClientRegistrationInfo.getDomainName();
            SchemeType domainScheme = extendedClientRegistrationInfo.getDomainScheme();
            domainsByInfoId.computeIfAbsent(extendedClientRegistrationInfo.getId(), key -> new ArrayList<>())
                    .add(new DomainInfo(domainScheme, domainName));
            infoById.put(extendedClientRegistrationInfo.getId(), extendedClientRegistrationInfo);
        }
        Map<List<DomainInfo>, OAuth2ClientsDomainParams> domainParamsMap = new LinkedHashMap<>();
        domainsByInfoId.forEach((clientRegistrationInfoId, domainInfos) -> {
            domainParamsMap.computeIfAbsent(domainInfos,
                    key -> new OAuth2ClientsDomainParams(key, new ArrayList<>())
            )
                    .getClientRegistrations()
                    .add(toClientRegistrationDto(infoById.get(clientRegistrationInfoId)));
        });
        boolean enabled = extendedOAuth2ClientRegistrationInfos.stream()
                .map(OAuth2ClientRegistrationInfo::isEnabled)
                .findFirst().orElse(false);
        return new OAuth2ClientsParams(enabled, new ArrayList<>(domainParamsMap.values()));
    }

    public static ClientRegistrationDto toClientRegistrationDto(OAuth2ClientRegistrationInfo oAuth2ClientRegistrationInfo) {
        return ClientRegistrationDto.builder()
                .mapperConfig(oAuth2ClientRegistrationInfo.getMapperConfig())
                .clientId(oAuth2ClientRegistrationInfo.getClientId())
                .clientSecret(oAuth2ClientRegistrationInfo.getClientSecret())
                .authorizationUri(oAuth2ClientRegistrationInfo.getAuthorizationUri())
                .accessTokenUri(oAuth2ClientRegistrationInfo.getAccessTokenUri())
                .scope(oAuth2ClientRegistrationInfo.getScope())
                .userInfoUri(oAuth2ClientRegistrationInfo.getUserInfoUri())
                .userNameAttributeName(oAuth2ClientRegistrationInfo.getUserNameAttributeName())
                .jwkSetUri(oAuth2ClientRegistrationInfo.getJwkSetUri())
                .clientAuthenticationMethod(oAuth2ClientRegistrationInfo.getClientAuthenticationMethod())
                .loginButtonLabel(oAuth2ClientRegistrationInfo.getLoginButtonLabel())
                .loginButtonIcon(oAuth2ClientRegistrationInfo.getLoginButtonIcon())
                .additionalInfo(oAuth2ClientRegistrationInfo.getAdditionalInfo())
                .build();
    }

    public static OAuth2ClientRegistrationInfo toClientRegistrationInfo(boolean enabled, ClientRegistrationDto clientRegistrationDto) {
        OAuth2ClientRegistrationInfo clientRegistrationInfo = new OAuth2ClientRegistrationInfo();
        clientRegistrationInfo.setEnabled(enabled);
        clientRegistrationInfo.setMapperConfig(clientRegistrationDto.getMapperConfig());
        clientRegistrationInfo.setClientId(clientRegistrationDto.getClientId());
        clientRegistrationInfo.setClientSecret(clientRegistrationDto.getClientSecret());
        clientRegistrationInfo.setAuthorizationUri(clientRegistrationDto.getAuthorizationUri());
        clientRegistrationInfo.setAccessTokenUri(clientRegistrationDto.getAccessTokenUri());
        clientRegistrationInfo.setScope(clientRegistrationDto.getScope());
        clientRegistrationInfo.setUserInfoUri(clientRegistrationDto.getUserInfoUri());
        clientRegistrationInfo.setUserNameAttributeName(clientRegistrationDto.getUserNameAttributeName());
        clientRegistrationInfo.setJwkSetUri(clientRegistrationDto.getJwkSetUri());
        clientRegistrationInfo.setClientAuthenticationMethod(clientRegistrationDto.getClientAuthenticationMethod());
        clientRegistrationInfo.setLoginButtonLabel(clientRegistrationDto.getLoginButtonLabel());
        clientRegistrationInfo.setLoginButtonIcon(clientRegistrationDto.getLoginButtonIcon());
        clientRegistrationInfo.setAdditionalInfo(clientRegistrationDto.getAdditionalInfo());
        return clientRegistrationInfo;
    }

    public static OAuth2ClientRegistration toClientRegistration(OAuth2ClientRegistrationInfoId clientRegistrationInfoId, SchemeType domainScheme, String domainName) {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setClientRegistrationId(clientRegistrationInfoId);
        clientRegistration.setDomainName(domainName);
        clientRegistration.setDomainScheme(domainScheme);
        return clientRegistration;
    }

    public static OAuth2ParamsInfo toOAuth2ParamsInfo(List<OAuth2Registration> registrations, List<OAuth2Domain> domains, List<OAuth2Mobile> mobiles) {
        OAuth2ParamsInfo oauth2ParamsInfo = new OAuth2ParamsInfo();
        oauth2ParamsInfo.setClientRegistrations(registrations.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2RegistrationInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setDomainInfos(domains.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2DomainInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setMobileInfos(mobiles.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2MobileInfo).collect(Collectors.toList()));
        return oauth2ParamsInfo;
    }

    public static OAuth2RegistrationInfo toOAuth2RegistrationInfo(OAuth2Registration registration) {
        return OAuth2RegistrationInfo.builder()
                .mapperConfig(registration.getMapperConfig())
                .clientId(registration.getClientId())
                .clientSecret(registration.getClientSecret())
                .authorizationUri(registration.getAuthorizationUri())
                .accessTokenUri(registration.getAccessTokenUri())
                .scope(registration.getScope())
                .platforms(registration.getPlatforms())
                .userInfoUri(registration.getUserInfoUri())
                .userNameAttributeName(registration.getUserNameAttributeName())
                .jwkSetUri(registration.getJwkSetUri())
                .clientAuthenticationMethod(registration.getClientAuthenticationMethod())
                .loginButtonLabel(registration.getLoginButtonLabel())
                .loginButtonIcon(registration.getLoginButtonIcon())
                .additionalInfo(registration.getAdditionalInfo())
                .build();
    }

    public static OAuth2DomainInfo toOAuth2DomainInfo(OAuth2Domain domain) {
        return OAuth2DomainInfo.builder()
                .name(domain.getDomainName())
                .scheme(domain.getDomainScheme())
                .build();
    }

    public static OAuth2MobileInfo toOAuth2MobileInfo(OAuth2Mobile mobile) {
        return OAuth2MobileInfo.builder()
                .pkgName(mobile.getPkgName())
                .appSecret(mobile.getAppSecret())
                .build();
    }

    public static OAuth2Params infoToOAuth2Params(OAuth2Info oauth2Info) {
        OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setEnabled(oauth2Info.isEnabled());
        oauth2Params.setTenantId(TenantId.SYS_TENANT_ID);
        return oauth2Params;
    }

    public static OAuth2Registration toOAuth2Registration(OAuth2ParamsId oauth2ParamsId, OAuth2RegistrationInfo registrationInfo) {
        OAuth2Registration registration = new OAuth2Registration();
        registration.setOauth2ParamsId(oauth2ParamsId);
        registration.setMapperConfig(registrationInfo.getMapperConfig());
        registration.setClientId(registrationInfo.getClientId());
        registration.setClientSecret(registrationInfo.getClientSecret());
        registration.setAuthorizationUri(registrationInfo.getAuthorizationUri());
        registration.setAccessTokenUri(registrationInfo.getAccessTokenUri());
        registration.setScope(registrationInfo.getScope());
        registration.setPlatforms(registrationInfo.getPlatforms());
        registration.setUserInfoUri(registrationInfo.getUserInfoUri());
        registration.setUserNameAttributeName(registrationInfo.getUserNameAttributeName());
        registration.setJwkSetUri(registrationInfo.getJwkSetUri());
        registration.setClientAuthenticationMethod(registrationInfo.getClientAuthenticationMethod());
        registration.setLoginButtonLabel(registrationInfo.getLoginButtonLabel());
        registration.setLoginButtonIcon(registrationInfo.getLoginButtonIcon());
        registration.setAdditionalInfo(registrationInfo.getAdditionalInfo());
        return registration;
    }

    public static OAuth2Domain toOAuth2Domain(OAuth2ParamsId oauth2ParamsId, OAuth2DomainInfo domainInfo) {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setOauth2ParamsId(oauth2ParamsId);
        domain.setDomainName(domainInfo.getName());
        domain.setDomainScheme(domainInfo.getScheme());
        return domain;
    }

    public static OAuth2Mobile toOAuth2Mobile(OAuth2ParamsId oauth2ParamsId, OAuth2MobileInfo mobileInfo) {
        OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setOauth2ParamsId(oauth2ParamsId);
        mobile.setPkgName(mobileInfo.getPkgName());
        mobile.setAppSecret(mobileInfo.getAppSecret());
        return mobile;
    }

    @Deprecated
    public static OAuth2Info clientParamsToOAuth2Info(OAuth2ClientsParams clientsParams) {
        OAuth2Info oauth2Info = new OAuth2Info();
        oauth2Info.setEnabled(clientsParams.isEnabled());
        oauth2Info.setOauth2ParamsInfos(clientsParams.getDomainsParams().stream().map(OAuth2Utils::clientsDomainParamsToOAuth2ParamsInfo).collect(Collectors.toList()));
        return oauth2Info;
    }

    private static OAuth2ParamsInfo clientsDomainParamsToOAuth2ParamsInfo(OAuth2ClientsDomainParams clientsDomainParams) {
        OAuth2ParamsInfo oauth2ParamsInfo = new OAuth2ParamsInfo();
        oauth2ParamsInfo.setMobileInfos(Collections.emptyList());
        oauth2ParamsInfo.setClientRegistrations(clientsDomainParams.getClientRegistrations().stream().map(OAuth2Utils::clientRegistrationDtoToOAuth2RegistrationInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setDomainInfos(clientsDomainParams.getDomainInfos().stream().map(OAuth2Utils::domainInfoToOAuth2DomainInfo).collect(Collectors.toList()));
        return oauth2ParamsInfo;
    }

    private static OAuth2RegistrationInfo clientRegistrationDtoToOAuth2RegistrationInfo(ClientRegistrationDto clientRegistrationDto) {
        return OAuth2RegistrationInfo.builder()
                .mapperConfig(clientRegistrationDto.getMapperConfig())
                .clientId(clientRegistrationDto.getClientId())
                .clientSecret(clientRegistrationDto.getClientSecret())
                .authorizationUri(clientRegistrationDto.getAuthorizationUri())
                .accessTokenUri(clientRegistrationDto.getAccessTokenUri())
                .scope(clientRegistrationDto.getScope())
                .userInfoUri(clientRegistrationDto.getUserInfoUri())
                .userNameAttributeName(clientRegistrationDto.getUserNameAttributeName())
                .jwkSetUri(clientRegistrationDto.getJwkSetUri())
                .clientAuthenticationMethod(clientRegistrationDto.getClientAuthenticationMethod())
                .loginButtonLabel(clientRegistrationDto.getLoginButtonLabel())
                .loginButtonIcon(clientRegistrationDto.getLoginButtonIcon())
                .additionalInfo(clientRegistrationDto.getAdditionalInfo())
                .platforms(Collections.emptyList())
                .build();
    }

    private static OAuth2DomainInfo domainInfoToOAuth2DomainInfo(DomainInfo domainInfo) {
        return OAuth2DomainInfo.builder()
                .name(domainInfo.getName())
                .scheme(domainInfo.getScheme())
                .build();
    }
}
