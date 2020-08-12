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
package org.thingsboard.server.common.data.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
public class OAuth2ClientRegistration extends BaseData<OAuth2ClientRegistrationId> implements HasTenantId, HasName {

    private TenantId tenantId;
    private String domainName;
    private String redirectUriTemplate;
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

    public OAuth2ClientRegistration(OAuth2ClientRegistration clientRegistration) {
        super(clientRegistration);
        this.tenantId = clientRegistration.tenantId;
        this.domainName = clientRegistration.domainName;
        this.redirectUriTemplate = clientRegistration.redirectUriTemplate;
        this.mapperConfig = clientRegistration.mapperConfig;
        this.clientId = clientRegistration.clientId;
        this.clientSecret = clientRegistration.clientSecret;
        this.authorizationUri = clientRegistration.authorizationUri;
        this.accessTokenUri = clientRegistration.accessTokenUri;
        this.scope = clientRegistration.scope;
        this.userInfoUri = clientRegistration.userInfoUri;
        this.userNameAttributeName = clientRegistration.userNameAttributeName;
        this.jwkSetUri = clientRegistration.jwkSetUri;
        this.clientAuthenticationMethod = clientRegistration.clientAuthenticationMethod;
        this.loginButtonLabel = clientRegistration.loginButtonLabel;
        this.loginButtonIcon = clientRegistration.loginButtonIcon;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return loginButtonLabel;
    }
}
