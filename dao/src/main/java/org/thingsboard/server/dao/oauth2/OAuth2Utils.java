/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.common.data.oauth2.OAuth2MobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;

import java.util.Comparator;
import java.util.List;
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
}
