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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.OAuth2IntegrationId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationEntity extends BaseSqlEntity<OAuth2ClientRegistration> {

    @Column(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_ID_PROPERTY)
    private String registrationId;
    @Column(name = ModelConstants.OAUTH2_CLIENT_ID_PROPERTY)
    private String clientId;
    @Column(name = ModelConstants.OAUTH2_CLIENT_SECRET_PROPERTY)
    private String clientSecret;
    @Column(name = ModelConstants.OAUTH2_AUTHORIZATION_URI_PROPERTY)
    private String authorizationUri;
    @Column(name = ModelConstants.OAUTH2_TOKEN_URI_PROPERTY)
    private String tokenUri;
    @Column(name = ModelConstants.OAUTH2_REDIRECT_URI_TEMPLATE_PROPERTY)
    private String redirectUriTemplate;
    @Column(name = ModelConstants.OAUTH2_SCOPE_PROPERTY)
    private String scope;
    @Column(name = ModelConstants.OAUTH2_AUTHORIZATION_GRANT_TYPE_PROPERTY)
    private String authorizationGrantType;
    @Column(name = ModelConstants.OAUTH2_USER_INFO_URI_PROPERTY)
    private String userInfoUri;
    @Column(name = ModelConstants.OAUTH2_USER_NAME_ATTRIBUTE_PROPERTY)
    private String userNameAttribute;
    @Column(name = ModelConstants.OAUTH2_JWK_SET_URI_PROPERTY)
    private String jwkSetUri;
    @Column(name = ModelConstants.OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY)
    private String clientAuthenticationMethod;
    @Column(name = ModelConstants.OAUTH2_CLIENT_NAME_PROPERTY)
    private String clientName;
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
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_STRATEGY_PROPERTY)
    private String tenantNameStrategy;
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

    public OAuth2ClientRegistrationEntity() {
        super();
    }

    @Override
    public OAuth2ClientRegistration toData() {
        return OAuth2ClientRegistration.builder()
                .id(new OAuth2IntegrationId(toUUID(id)))
                .registrationId(registrationId)
                .mapperConfig(OAuth2MapperConfig.builder()
                        .allowUserCreation(allowUserCreation)
                        .activateUser(activateUser)
                        .type(type)
                        .basicConfig(
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
                        )
                        .customConfig(
                                OAuth2CustomMapperConfig.builder()
                                        .url(url)
                                        .username(username)
                                        .password(password)
                                        .build()
                        )
                        .build())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .redirectUriTemplate(redirectUriTemplate)
                .scope(scope)
                .authorizationGrantType(authorizationGrantType)
                .userInfoUri(userInfoUri)
                .userNameAttribute(userNameAttribute)
                .jwkSetUri(jwkSetUri)
                .clientAuthenticationMethod(clientAuthenticationMethod)
                .clientName(clientName)
                .loginButtonLabel(loginButtonLabel)
                .loginButtonIcon(loginButtonIcon)
                .build();
    }
}
