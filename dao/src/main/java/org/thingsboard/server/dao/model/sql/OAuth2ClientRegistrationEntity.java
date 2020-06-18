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
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationEntity extends BaseSqlEntity<OAuth2ClientRegistration> {

    @Column(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_ID_PROPERTY)
    private String registrationId;
    @Column(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_MAPPER_CONFIG_ID_PROPERTY)
    private String mapperConfigId;
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

    public OAuth2ClientRegistrationEntity() {
        super();
    }

    @Override
    public OAuth2ClientRegistration toData() {
        return OAuth2ClientRegistration.builder()
                .id(new OAuth2IntegrationId(toUUID(id)))
                .registrationId(registrationId)
                .mapperConfigId(new OAuth2IntegrationId(toUUID(mapperConfigId)))
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
