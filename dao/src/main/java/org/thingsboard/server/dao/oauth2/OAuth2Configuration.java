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
package org.thingsboard.server.dao.oauth2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "security.oauth2")
@Data
@Slf4j
public class OAuth2Configuration {

    private boolean enabled;
    private String loginProcessingUrl;
    private Map<String, OAuth2Client> clients = new HashMap<>();

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> result = new ArrayList<>();
        for (Map.Entry<String, OAuth2Client> entry : clients.entrySet()) {
            OAuth2Client client = entry.getValue();
            ClientRegistration registration = ClientRegistration.withRegistrationId(entry.getKey())
                    .clientId(client.getClientId())
                    .authorizationUri(client.getAuthorizationUri())
                    .clientSecret(client.getClientSecret())
                    .tokenUri(client.getAccessTokenUri())
                    .redirectUriTemplate(client.getRedirectUriTemplate())
                    .scope(client.getScope().split(","))
                    .clientName(client.getClientName())
                    .authorizationGrantType(new AuthorizationGrantType(client.getAuthorizationGrantType()))
                    .userInfoUri(client.getUserInfoUri())
                    .userNameAttributeName(client.getUserNameAttributeName())
                    .jwkSetUri(client.getJwkSetUri())
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getClientAuthenticationMethod()))
                    .build();
            result.add(registration);
        }
        return new InMemoryClientRegistrationRepository(result);
    }
}
