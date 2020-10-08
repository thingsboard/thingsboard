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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractOAuth2ClientRegistrationInfoEntity<T extends OAuth2ClientRegistrationInfo> extends BaseSqlEntity<T> {

    @Column(name = ModelConstants.OAUTH2_ENABLED_PROPERTY)
    private Boolean enabled;
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

    public AbstractOAuth2ClientRegistrationInfoEntity() {
        super();
    }

    public AbstractOAuth2ClientRegistrationInfoEntity(OAuth2ClientRegistrationInfo clientRegistrationInfo) {
        if (clientRegistrationInfo.getId() != null) {
            this.setUuid(clientRegistrationInfo.getId().getId());
        }
        this.createdTime = clientRegistrationInfo.getCreatedTime();
        this.enabled = clientRegistrationInfo.isEnabled();
        this.clientId = clientRegistrationInfo.getClientId();
        this.clientSecret = clientRegistrationInfo.getClientSecret();
        this.authorizationUri = clientRegistrationInfo.getAuthorizationUri();
        this.tokenUri = clientRegistrationInfo.getAccessTokenUri();
        this.scope = clientRegistrationInfo.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.userInfoUri = clientRegistrationInfo.getUserInfoUri();
        this.userNameAttributeName = clientRegistrationInfo.getUserNameAttributeName();
        this.jwkSetUri = clientRegistrationInfo.getJwkSetUri();
        this.clientAuthenticationMethod = clientRegistrationInfo.getClientAuthenticationMethod();
        this.loginButtonLabel = clientRegistrationInfo.getLoginButtonLabel();
        this.loginButtonIcon = clientRegistrationInfo.getLoginButtonIcon();
        this.additionalInfo = clientRegistrationInfo.getAdditionalInfo();
        OAuth2MapperConfig mapperConfig = clientRegistrationInfo.getMapperConfig();
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

    public AbstractOAuth2ClientRegistrationInfoEntity(OAuth2ClientRegistrationInfoEntity oAuth2ClientRegistrationInfoEntity) {
        this.setId(oAuth2ClientRegistrationInfoEntity.getId());
        this.setCreatedTime(oAuth2ClientRegistrationInfoEntity.getCreatedTime());
        this.enabled = oAuth2ClientRegistrationInfoEntity.getEnabled();
        this.clientId = oAuth2ClientRegistrationInfoEntity.getClientId();
        this.clientSecret = oAuth2ClientRegistrationInfoEntity.getClientSecret();
        this.authorizationUri = oAuth2ClientRegistrationInfoEntity.getAuthorizationUri();
        this.tokenUri = oAuth2ClientRegistrationInfoEntity.getTokenUri();
        this.scope = oAuth2ClientRegistrationInfoEntity.getScope();
        this.userInfoUri = oAuth2ClientRegistrationInfoEntity.getUserInfoUri();
        this.userNameAttributeName = oAuth2ClientRegistrationInfoEntity.getUserNameAttributeName();
        this.jwkSetUri = oAuth2ClientRegistrationInfoEntity.getJwkSetUri();
        this.clientAuthenticationMethod = oAuth2ClientRegistrationInfoEntity.getClientAuthenticationMethod();
        this.loginButtonLabel = oAuth2ClientRegistrationInfoEntity.getLoginButtonLabel();
        this.loginButtonIcon = oAuth2ClientRegistrationInfoEntity.getLoginButtonIcon();
        this.additionalInfo = oAuth2ClientRegistrationInfoEntity.getAdditionalInfo();
        this.allowUserCreation = oAuth2ClientRegistrationInfoEntity.getAllowUserCreation();
        this.activateUser = oAuth2ClientRegistrationInfoEntity.getActivateUser();
        this.type = oAuth2ClientRegistrationInfoEntity.getType();
        this.emailAttributeKey = oAuth2ClientRegistrationInfoEntity.getEmailAttributeKey();
        this.firstNameAttributeKey = oAuth2ClientRegistrationInfoEntity.getFirstNameAttributeKey();
        this.lastNameAttributeKey = oAuth2ClientRegistrationInfoEntity.getLastNameAttributeKey();
        this.tenantNameStrategy = oAuth2ClientRegistrationInfoEntity.getTenantNameStrategy();
        this.tenantNamePattern = oAuth2ClientRegistrationInfoEntity.getTenantNamePattern();
        this.customerNamePattern = oAuth2ClientRegistrationInfoEntity.getCustomerNamePattern();
        this.defaultDashboardName = oAuth2ClientRegistrationInfoEntity.getDefaultDashboardName();
        this.alwaysFullScreen = oAuth2ClientRegistrationInfoEntity.getAlwaysFullScreen();
        this.url = oAuth2ClientRegistrationInfoEntity.getUrl();
        this.username = oAuth2ClientRegistrationInfoEntity.getUsername();
        this.password = oAuth2ClientRegistrationInfoEntity.getPassword();
        this.sendToken = oAuth2ClientRegistrationInfoEntity.getSendToken();
    }


    protected OAuth2ClientRegistrationInfo toOAuth2ClientRegistrationInfo() {
        OAuth2ClientRegistrationInfo clientRegistrationInfo = new OAuth2ClientRegistrationInfo();
        clientRegistrationInfo.setId(new OAuth2ClientRegistrationInfoId(id));
        clientRegistrationInfo.setEnabled(enabled);
        clientRegistrationInfo.setCreatedTime(createdTime);
        clientRegistrationInfo.setAdditionalInfo(additionalInfo);
        clientRegistrationInfo.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(allowUserCreation)
                        .activateUser(activateUser)
                        .type(type)
                        .basic(
                                (type == MapperType.BASIC || type == MapperType.GITHUB) ?
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
        clientRegistrationInfo.setClientId(clientId);
        clientRegistrationInfo.setClientSecret(clientSecret);
        clientRegistrationInfo.setAuthorizationUri(authorizationUri);
        clientRegistrationInfo.setAccessTokenUri(tokenUri);
        clientRegistrationInfo.setScope(Arrays.asList(scope.split(",")));
        clientRegistrationInfo.setUserInfoUri(userInfoUri);
        clientRegistrationInfo.setUserNameAttributeName(userNameAttributeName);
        clientRegistrationInfo.setJwkSetUri(jwkSetUri);
        clientRegistrationInfo.setClientAuthenticationMethod(clientAuthenticationMethod);
        clientRegistrationInfo.setLoginButtonLabel(loginButtonLabel);
        clientRegistrationInfo.setLoginButtonIcon(loginButtonIcon);
        return clientRegistrationInfo;
    }
}
