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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2ClientRegistrationTemplate extends SearchTextBasedWithAdditionalInfo<OAuth2ClientRegistrationTemplateId> implements HasName {

    private String providerId;
    private OAuth2MapperConfig mapperConfig;
    private String authorizationUri;
    private String accessTokenUri;
    private List<String> scope;
    private String userInfoUri;
    private String userNameAttributeName;
    private String jwkSetUri;
    private String clientAuthenticationMethod;
    private String comment;
    private String loginButtonIcon;
    private String loginButtonLabel;
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
