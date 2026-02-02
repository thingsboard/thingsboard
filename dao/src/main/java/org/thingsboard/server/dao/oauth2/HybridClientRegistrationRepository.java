/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;

import java.util.UUID;

@Component
public class HybridClientRegistrationRepository implements ClientRegistrationRepository {

    private static final String defaultRedirectUriTemplate = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Autowired
    private OAuth2ClientService oAuth2ClientService;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        OAuth2Client oAuth2Client = oAuth2ClientService.findOAuth2ClientById(TenantId.SYS_TENANT_ID, new OAuth2ClientId(UUID.fromString(registrationId)));
        if (oAuth2Client == null) {
            return null;
        }
        return toSpringClientRegistration(oAuth2Client);
    }

    private ClientRegistration toSpringClientRegistration(OAuth2Client oAuth2Client) {
        String registrationId = oAuth2Client.getUuidId().toString();

        // NONE is used if we need pkce-based code challenge
        ClientAuthenticationMethod authMethod = ClientAuthenticationMethod.NONE;
        if (oAuth2Client.getClientAuthenticationMethod().equals("POST")) {
            authMethod = ClientAuthenticationMethod.CLIENT_SECRET_POST;
        } else if (oAuth2Client.getClientAuthenticationMethod().equals("BASIC")) {
            authMethod = ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }

        return ClientRegistration.withRegistrationId(registrationId)
                .clientName(oAuth2Client.getName())
                .clientId(oAuth2Client.getClientId())
                .authorizationUri(oAuth2Client.getAuthorizationUri())
                .clientSecret(oAuth2Client.getClientSecret())
                .tokenUri(oAuth2Client.getAccessTokenUri())
                .scope(oAuth2Client.getScope())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .userInfoUri(oAuth2Client.getUserInfoUri())
                .userNameAttributeName(oAuth2Client.getUserNameAttributeName())
                .jwkSetUri(oAuth2Client.getJwkSetUri())
                .clientAuthenticationMethod(authMethod)
                .redirectUri(defaultRedirectUriTemplate)
                .build();
    }

}
