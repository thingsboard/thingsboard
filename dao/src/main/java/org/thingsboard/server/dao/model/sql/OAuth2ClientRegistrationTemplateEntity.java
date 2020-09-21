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
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;

import javax.persistence.*;
import java.util.Arrays;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_TEMPLATE_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationTemplateEntity extends BaseSqlEntity<OAuth2ClientRegistrationTemplate> {

    @Column(name = ModelConstants.OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY)
    private String providerId;
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
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_COMMENT_PROPERTY)
    private String comment;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY)
    private String loginButtonIcon;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_LABEL_PROPERTY)
    private String loginButtonLabel;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_HELP_LINK_PROPERTY)
    private String helpLink;

    @Type(type = "json")
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public OAuth2ClientRegistrationTemplateEntity() {
    }

    public OAuth2ClientRegistrationTemplateEntity(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (clientRegistrationTemplate.getId() != null) {
            this.setUuid(clientRegistrationTemplate.getId().getId());
        }
        this.createdTime = clientRegistrationTemplate.getCreatedTime();
        this.providerId = clientRegistrationTemplate.getProviderId();
        this.authorizationUri = clientRegistrationTemplate.getAuthorizationUri();
        this.tokenUri = clientRegistrationTemplate.getAccessTokenUri();
        this.scope = clientRegistrationTemplate.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.userInfoUri = clientRegistrationTemplate.getUserInfoUri();
        this.userNameAttributeName = clientRegistrationTemplate.getUserNameAttributeName();
        this.jwkSetUri = clientRegistrationTemplate.getJwkSetUri();
        this.clientAuthenticationMethod = clientRegistrationTemplate.getClientAuthenticationMethod();
        this.comment = clientRegistrationTemplate.getComment();
        this.loginButtonIcon = clientRegistrationTemplate.getLoginButtonIcon();
        this.loginButtonLabel = clientRegistrationTemplate.getLoginButtonLabel();
        this.helpLink = clientRegistrationTemplate.getHelpLink();
        this.additionalInfo = clientRegistrationTemplate.getAdditionalInfo();
        OAuth2BasicMapperConfig basicConfig = clientRegistrationTemplate.getBasic();
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
    }

    @Override
    public OAuth2ClientRegistrationTemplate toData() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setId(new OAuth2ClientRegistrationTemplateId(id));
        clientRegistrationTemplate.setCreatedTime(createdTime);
        clientRegistrationTemplate.setAdditionalInfo(additionalInfo);

        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setBasic(
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
        );
        clientRegistrationTemplate.setAuthorizationUri(authorizationUri);
        clientRegistrationTemplate.setAccessTokenUri(tokenUri);
        clientRegistrationTemplate.setScope(Arrays.asList(scope.split(",")));
        clientRegistrationTemplate.setUserInfoUri(userInfoUri);
        clientRegistrationTemplate.setUserNameAttributeName(userNameAttributeName);
        clientRegistrationTemplate.setJwkSetUri(jwkSetUri);
        clientRegistrationTemplate.setClientAuthenticationMethod(clientAuthenticationMethod);
        clientRegistrationTemplate.setComment(comment);
        clientRegistrationTemplate.setLoginButtonIcon(loginButtonIcon);
        clientRegistrationTemplate.setLoginButtonLabel(loginButtonLabel);
        clientRegistrationTemplate.setHelpLink(helpLink);
        return clientRegistrationTemplate;
    }
}
