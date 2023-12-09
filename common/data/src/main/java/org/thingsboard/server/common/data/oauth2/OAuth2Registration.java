/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
public class OAuth2Registration extends BaseDataWithAdditionalInfo<OAuth2RegistrationId> implements HasName {

    private OAuth2ParamsId oauth2ParamsId;
    private OAuth2MapperConfig mapperConfig;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String accessTokenUri;
    private List<String> scope;
    private String userInfoUri;
    private String userNameAttributeName;
    private String jwkSetUri;
    private String clientAuthenticationMethod;
    private String loginButtonLabel;
    private String loginButtonIcon;
    private List<PlatformType> platforms;

    public OAuth2Registration(OAuth2Registration registration) {
        super(registration);
        this.oauth2ParamsId = registration.oauth2ParamsId;
        this.mapperConfig = registration.mapperConfig;
        this.clientId = registration.clientId;
        this.clientSecret = registration.clientSecret;
        this.authorizationUri = registration.authorizationUri;
        this.accessTokenUri = registration.accessTokenUri;
        this.scope = registration.scope;
        this.userInfoUri = registration.userInfoUri;
        this.userNameAttributeName = registration.userNameAttributeName;
        this.jwkSetUri = registration.jwkSetUri;
        this.clientAuthenticationMethod = registration.clientAuthenticationMethod;
        this.loginButtonLabel = registration.loginButtonLabel;
        this.loginButtonIcon = registration.loginButtonIcon;
        this.platforms = registration.platforms;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return loginButtonLabel;
    }
}
