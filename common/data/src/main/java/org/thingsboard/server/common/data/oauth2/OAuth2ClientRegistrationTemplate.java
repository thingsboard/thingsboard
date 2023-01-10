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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.validation.Length;

import javax.validation.Valid;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@ApiModel
public class OAuth2ClientRegistrationTemplate extends SearchTextBasedWithAdditionalInfo<OAuth2ClientRegistrationTemplateId> implements HasName {

    @Length(fieldName = "providerId")
    @ApiModelProperty(value = "OAuth2 provider identifier (e.g. its name)", required = true)
    private String providerId;
    @Valid
    @ApiModelProperty(value = "Default config for mapping OAuth2 log in response to platform entities")
    private OAuth2MapperConfig mapperConfig;
    @Length(fieldName = "authorizationUri")
    @ApiModelProperty(value = "Default authorization URI of the OAuth2 provider")
    private String authorizationUri;
    @Length(fieldName = "accessTokenUri")
    @ApiModelProperty(value = "Default access token URI of the OAuth2 provider")
    private String accessTokenUri;
    @ApiModelProperty(value = "Default OAuth scopes that will be requested from OAuth2 platform")
    private List<String> scope;
    @Length(fieldName = "userInfoUri")
    @ApiModelProperty(value = "Default user info URI of the OAuth2 provider")
    private String userInfoUri;
    @Length(fieldName = "userNameAttributeName")
    @ApiModelProperty(value = "Default name of the username attribute in OAuth2 provider log in response")
    private String userNameAttributeName;
    @Length(fieldName = "jwkSetUri")
    @ApiModelProperty(value = "Default JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    @Length(fieldName = "clientAuthenticationMethod")
    @ApiModelProperty(value = "Default client authentication method to use: 'BASIC' or 'POST'")
    private String clientAuthenticationMethod;
    @ApiModelProperty(value = "Comment for OAuth2 provider")
    private String comment;
    @Length(fieldName = "loginButtonIcon")
    @ApiModelProperty(value = "Default log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    @Length(fieldName = "loginButtonLabel")
    @ApiModelProperty(value = "Default OAuth2 provider label")
    private String loginButtonLabel;
    @Length(fieldName = "helpLink")
    @ApiModelProperty(value = "Help link for OAuth2 provider")
    private String helpLink;

    public OAuth2ClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        super(clientRegistrationTemplate);
        this.providerId = clientRegistrationTemplate.providerId;
        this.mapperConfig = clientRegistrationTemplate.mapperConfig;
        this.authorizationUri = clientRegistrationTemplate.authorizationUri;
        this.accessTokenUri = clientRegistrationTemplate.accessTokenUri;
        this.scope = clientRegistrationTemplate.scope;
        this.userInfoUri = clientRegistrationTemplate.userInfoUri;
        this.userNameAttributeName = clientRegistrationTemplate.userNameAttributeName;
        this.jwkSetUri = clientRegistrationTemplate.jwkSetUri;
        this.clientAuthenticationMethod = clientRegistrationTemplate.clientAuthenticationMethod;
        this.comment = clientRegistrationTemplate.comment;
        this.loginButtonIcon = clientRegistrationTemplate.loginButtonIcon;
        this.loginButtonLabel = clientRegistrationTemplate.loginButtonLabel;
        this.helpLink = clientRegistrationTemplate.helpLink;
    }

    @Override
    public String getName() {
        return providerId;
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
