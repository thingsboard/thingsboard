// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.oauth2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@Schema
public class OAuth2ClientRegistrationTemplate extends BaseDataWithAdditionalInfo<OAuth2ClientRegistrationTemplateId> implements HasName {

    @Length(fieldName = "providerId")
    @Schema(description = "OAuth2 provider identifier (e.g. its name)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String providerId;
    @Valid
    @Schema(description = "Default config for mapping OAuth2 log in response to platform entities")
    private OAuth2MapperConfig mapperConfig;
    @Length(fieldName = "authorizationUri")
    @Schema(description = "Default authorization URI of the OAuth2 provider")
    private String authorizationUri;
    @Length(fieldName = "accessTokenUri")
    @Schema(description = "Default access token URI of the OAuth2 provider")
    private String accessTokenUri;
    @Schema(description = "Default OAuth scopes that will be requested from OAuth2 platform")
    private List<String> scope;
    @Length(fieldName = "userInfoUri")
    @Schema(description = "Default user info URI of the OAuth2 provider")
    private String userInfoUri;
    @Length(fieldName = "userNameAttributeName")
    @Schema(description = "Default name of the username attribute in OAuth2 provider log in response")
    private String userNameAttributeName;
    @Length(fieldName = "jwkSetUri")
    @Schema(description = "Default JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    @Length(fieldName = "clientAuthenticationMethod")
    @Schema(description = "Default client authentication method to use: 'BASIC' or 'POST'")
    private String clientAuthenticationMethod;
    @Schema(description = "Comment for OAuth2 provider")
    private String comment;
    @Length(fieldName = "loginButtonIcon")
    @Schema(description = "Default log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    @Length(fieldName = "loginButtonLabel")
    @Schema(description = "Default OAuth2 provider label")
    private String loginButtonLabel;
    @Length(fieldName = "helpLink")
    @Schema(description = "Help link for OAuth2 provider")
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
}
