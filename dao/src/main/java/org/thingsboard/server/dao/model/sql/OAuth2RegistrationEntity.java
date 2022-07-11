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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_REGISTRATION_COLUMN_FAMILY_NAME)
public class OAuth2RegistrationEntity extends BaseSqlEntity<OAuth2Registration> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;
    @Column(name = ModelConstants.OAUTH2_CLIENT_ID_PROPERTY)
    private String clientId;
    @Column(name = ModelConstants.OAUTH2_CLIENT_SECRET_PROPERTY)
    private String clientSecret;
    @Column(name = ModelConstants.OAUTH2_AUTHORIZATION_URI_PROPERTY)
    private String authorizationUri;
    @Column(name = ModelConstants.OAUTH2_TOKEN_URI_PROPERTY)
    private String tokenUri;
    @Column(name = ModelConstants.OAUTH2_SCOPE_PROPERTY)
    private String scope;
    @Column(name = ModelConstants.OAUTH2_PLATFORMS_PROPERTY)
    private String platforms;
    @Column(name = ModelConstants.OAUTH2_USER_INFO_URI_PROPERTY)
    private String userInfoUri;
    @Column(name = ModelConstants.OAUTH2_USER_NAME_ATTRIBUTE_NAME_PROPERTY)
    private String userNameAttributeName;
    @Column(name = ModelConstants.OAUTH2_JWK_SET_URI_PROPERTY)
    private String jwkSetUri;
    @Column(name = ModelConstants.OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY)
    private String clientAuthenticationMethod;
    @Column(name = ModelConstants.OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY)
    private String loginButtonLabel;
    @Column(name = ModelConstants.OAUTH2_LOGIN_BUTTON_ICON_PROPERTY)
    private String loginButtonIcon;
    @Column(name = ModelConstants.OAUTH2_ALLOW_USER_CREATION_PROPERTY)
    private Boolean allowUserCreation;
    @Column(name = ModelConstants.OAUTH2_ACTIVATE_USER_PROPERTY)
    private Boolean activateUser;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_MAPPER_TYPE_PROPERTY)
    private MapperType type;
    @Column(name = ModelConstants.OAUTH2_EMAIL_ATTRIBUTE_KEY_PROPERTY)
    private String emailAttributeKey;
    @Column(name = ModelConstants.OAUTH2_FIRST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String firstNameAttributeKey;
    @Column(name = ModelConstants.OAUTH2_LAST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String lastNameAttributeKey;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_STRATEGY_PROPERTY)
    private TenantNameStrategyType tenantNameStrategy;
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_PATTERN_PROPERTY)
    private String tenantNamePattern;
    @Column(name = ModelConstants.OAUTH2_CUSTOMER_NAME_PATTERN_PROPERTY)
    private String customerNamePattern;
    @Column(name = ModelConstants.OAUTH2_DEFAULT_DASHBOARD_NAME_PROPERTY)
    private String defaultDashboardName;
    @Column(name = ModelConstants.OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY)
    private Boolean alwaysFullScreen;
    @Column(name = ModelConstants.OAUTH2_MAPPER_URL_PROPERTY)
    private String url;
    @Column(name = ModelConstants.OAUTH2_MAPPER_USERNAME_PROPERTY)
    private String username;
    @Column(name = ModelConstants.OAUTH2_MAPPER_PASSWORD_PROPERTY)
    private String password;
    @Column(name = ModelConstants.OAUTH2_MAPPER_SEND_TOKEN_PROPERTY)
    private Boolean sendToken;

    @Type(type = "json")
    @Column(name = ModelConstants.OAUTH2_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public OAuth2RegistrationEntity() {
        super();
    }

    public OAuth2RegistrationEntity(OAuth2Registration registration) {
        if (registration.getId() != null) {
            this.setUuid(registration.getId().getId());
        }
        this.setCreatedTime(registration.getCreatedTime());
        if (registration.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = registration.getOauth2ParamsId().getId();
        }
        this.clientId = registration.getClientId();
        this.clientSecret = registration.getClientSecret();
        this.authorizationUri = registration.getAuthorizationUri();
        this.tokenUri = registration.getAccessTokenUri();
        this.scope = registration.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.platforms = registration.getPlatforms() != null ? registration.getPlatforms().stream().map(Enum::name).reduce((result, element) -> result + "," + element).orElse("") : "";
        this.userInfoUri = registration.getUserInfoUri();
        this.userNameAttributeName = registration.getUserNameAttributeName();
        this.jwkSetUri = registration.getJwkSetUri();
        this.clientAuthenticationMethod = registration.getClientAuthenticationMethod();
        this.loginButtonLabel = registration.getLoginButtonLabel();
        this.loginButtonIcon = registration.getLoginButtonIcon();
        this.additionalInfo = registration.getAdditionalInfo();
        OAuth2MapperConfig mapperConfig = registration.getMapperConfig();
        if (mapperConfig != null) {
            this.allowUserCreation = mapperConfig.isAllowUserCreation();
            this.activateUser = mapperConfig.isActivateUser();
            this.type = mapperConfig.getType();
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
            if (basicConfig != null) {
                this.emailAttributeKey = basicConfig.getEmailAttributeKey();
                this.firstNameAttributeKey = basicConfig.getFirstNameAttributeKey();
                this.lastNameAttributeKey = basicConfig.getLastNameAttributeKey();
                this.tenantNameStrategy = basicConfig.getTenantNameStrategy();
                this.tenantNamePattern = basicConfig.getTenantNamePattern();
                this.customerNamePattern = basicConfig.getCustomerNamePattern();
                this.defaultDashboardName = basicConfig.getDefaultDashboardName();
                this.alwaysFullScreen = basicConfig.isAlwaysFullScreen();
            }
            OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
            if (customConfig != null) {
                this.url = customConfig.getUrl();
                this.username = customConfig.getUsername();
                this.password = customConfig.getPassword();
                this.sendToken = customConfig.isSendToken();
            }
        }
    }

    @Override
    public OAuth2Registration toData() {
        OAuth2Registration registration = new OAuth2Registration();
        registration.setId(new OAuth2RegistrationId(id));
        registration.setCreatedTime(createdTime);
        registration.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        registration.setAdditionalInfo(additionalInfo);
        registration.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(allowUserCreation)
                        .activateUser(activateUser)
                        .type(type)
                        .basic(
                                (type == MapperType.BASIC || type == MapperType.GITHUB || type == MapperType.APPLE) ?
                                        OAuth2BasicMapperConfig.builder()
                                                .emailAttributeKey(emailAttributeKey)
                                                .firstNameAttributeKey(firstNameAttributeKey)
                                                .lastNameAttributeKey(lastNameAttributeKey)
                                                .tenantNameStrategy(tenantNameStrategy)
                                                .tenantNamePattern(tenantNamePattern)
                                                .customerNamePattern(customerNamePattern)
                                                .defaultDashboardName(defaultDashboardName)
                                                .alwaysFullScreen(alwaysFullScreen)
                                                .build()
                                        : null
                        )
                        .custom(
                                type == MapperType.CUSTOM ?
                                        OAuth2CustomMapperConfig.builder()
                                                .url(url)
                                                .username(username)
                                                .password(password)
                                                .sendToken(sendToken)
                                                .build()
                                        : null
                        )
                        .build()
        );
        registration.setClientId(clientId);
        registration.setClientSecret(clientSecret);
        registration.setAuthorizationUri(authorizationUri);
        registration.setAccessTokenUri(tokenUri);
        registration.setScope(Arrays.asList(scope.split(",")));
        registration.setPlatforms(StringUtils.isNotEmpty(platforms) ? Arrays.stream(platforms.split(","))
                .map(str -> PlatformType.valueOf(str)).collect(Collectors.toList()) : Collections.emptyList());
        registration.setUserInfoUri(userInfoUri);
        registration.setUserNameAttributeName(userNameAttributeName);
        registration.setJwkSetUri(jwkSetUri);
        registration.setClientAuthenticationMethod(clientAuthenticationMethod);
        registration.setLoginButtonLabel(loginButtonLabel);
        registration.setLoginButtonIcon(loginButtonIcon);
        return registration;
    }
}
