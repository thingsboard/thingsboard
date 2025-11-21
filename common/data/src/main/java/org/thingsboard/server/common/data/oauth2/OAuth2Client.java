/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
public class OAuth2Client extends BaseDataWithAdditionalInfo<OAuth2ClientId> implements HasName, HasTenantId {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Oauth2 client title")
    @NotBlank
    @NoXss
    @Length(fieldName = "title", max = 100, message = "cannot be longer than 100 chars")
    private String title;
    @Schema(description = "Config for mapping OAuth2 log in response to platform entities", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private OAuth2MapperConfig mapperConfig;
    @Schema(description = "OAuth2 client ID. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientId")
    private String clientId;
    @Schema(description = "OAuth2 client secret. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientSecret", max = 2048)
    private String clientSecret;
    @Schema(description = "Authorization URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "authorizationUri")
    private String authorizationUri;
    @Schema(description = "Access token URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "accessTokenUri")
    private String accessTokenUri;
    @Schema(description = "OAuth scopes that will be requested from OAuth2 platform. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Length(fieldName = "scope")
    private List<String> scope;
    @Schema(description = "User info URI of the OAuth2 provider")
    @Length(fieldName = "userInfoUri")
    private String userInfoUri;
    @Schema(description = "Name of the username attribute in OAuth2 provider response. Cannot be empty")
    @NotBlank
    @Length(fieldName = "userNameAttributeName")
    private String userNameAttributeName;
    @Schema(description = "JSON Web Key URI of the OAuth2 provider")
    @Length(fieldName = "jwkSetUri")
    private String jwkSetUri;
    @Schema(description = "Client authentication method to use: 'BASIC' or 'POST'. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientAuthenticationMethod")
    private String clientAuthenticationMethod;
    @Schema(description = "OAuth2 provider label. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "loginButtonLabel")
    private String loginButtonLabel;
    @Schema(description = "Log in button icon for OAuth2 provider")
    @Length(fieldName = "loginButtonIcon")
    private String loginButtonIcon;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    @Length(fieldName = "platforms")
    private List<PlatformType> platforms;
    @Schema(description = "Additional info of OAuth2 client (e.g. providerName)", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode additionalInfo;

    public OAuth2Client() {
        super();
    }

    public OAuth2Client(OAuth2ClientId id) {
        super(id);
    }

    public OAuth2Client(OAuth2Client oAuth2Client) {
        super(oAuth2Client);
        this.tenantId = oAuth2Client.tenantId;
        this.title = oAuth2Client.title;
        this.mapperConfig = oAuth2Client.mapperConfig;
        this.clientId = oAuth2Client.clientId;
        this.clientSecret = oAuth2Client.clientSecret;
        this.authorizationUri = oAuth2Client.authorizationUri;
        this.accessTokenUri = oAuth2Client.accessTokenUri;
        this.scope = oAuth2Client.scope;
        this.userInfoUri = oAuth2Client.userInfoUri;
        this.userNameAttributeName = oAuth2Client.userNameAttributeName;
        this.jwkSetUri = oAuth2Client.jwkSetUri;
        this.clientAuthenticationMethod = oAuth2Client.clientAuthenticationMethod;
        this.loginButtonLabel = oAuth2Client.loginButtonLabel;
        this.loginButtonIcon = oAuth2Client.loginButtonIcon;
        this.platforms = oAuth2Client.platforms;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }
}
