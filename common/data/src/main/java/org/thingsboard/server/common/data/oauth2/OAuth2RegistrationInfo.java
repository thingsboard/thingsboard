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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel
public class OAuth2RegistrationInfo {
    @ApiModelProperty(value = "Config for mapping OAuth2 log in response to platform entities", required = true)
    private OAuth2MapperConfig mapperConfig;
    @ApiModelProperty(value = "OAuth2 client ID. Cannot be empty", required = true)
    private String clientId;
    @ApiModelProperty(value = "OAuth2 client secret. Cannot be empty", required = true)
    private String clientSecret;
    @ApiModelProperty(value = "Authorization URI of the OAuth2 provider. Cannot be empty", required = true)
    private String authorizationUri;
    @ApiModelProperty(value = "Access token URI of the OAuth2 provider. Cannot be empty", required = true)
    private String accessTokenUri;
    @ApiModelProperty(value = "OAuth scopes that will be requested from OAuth2 platform. Cannot be empty", required = true)
    private List<String> scope;
    @ApiModelProperty(value = "User info URI of the OAuth2 provider")
    private String userInfoUri;
    @ApiModelProperty(value = "Name of the username attribute in OAuth2 provider response. Cannot be empty")
    private String userNameAttributeName;
    @ApiModelProperty(value = "JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    @ApiModelProperty(value = "Client authentication method to use: 'BASIC' or 'POST'. Cannot be empty", required = true)
    private String clientAuthenticationMethod;
    @ApiModelProperty(value = "OAuth2 provider label. Cannot be empty", required = true)
    private String loginButtonLabel;
    @ApiModelProperty(value = "Log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    @ApiModelProperty(value = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;
    @ApiModelProperty(value = "Additional info of OAuth2 client (e.g. providerName)", required = true)
    private JsonNode additionalInfo;
}
