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
package org.thingsboard.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Collections;

@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
@Configuration
public class ThingsboardOAuth2Configuration {

    @Value("${security.oauth2.registrationId}")
    private String registrationId;
    @Value("${security.oauth2.userNameAttributeName}")
    private String userNameAttributeName;

    @Value("${security.oauth2.client.clientId}")
    private String clientId;
    @Value("${security.oauth2.client.clientName}")
    private String clientName;
    @Value("${security.oauth2.client.clientSecret}")
    private String clientSecret;
    @Value("${security.oauth2.client.accessTokenUri}")
    private String accessTokenUri;
    @Value("${security.oauth2.client.authorizationUri}")
    private String authorizationUri;
    @Value("${security.oauth2.client.redirectUriTemplate}")
    private String redirectUriTemplate;
    @Value("${security.oauth2.client.scope}")
    private String scope;
    @Value("${security.oauth2.client.jwkSetUri}")
    private String jwkSetUri;
    @Value("${security.oauth2.client.authorizationGrantType}")
    private String authorizationGrantType;
    @Value("${security.oauth2.client.clientAuthenticationMethod}")
    private String clientAuthenticationMethod;

    @Value("${security.oauth2.resource.userInfoUri}")
    private String userInfoUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .authorizationUri(authorizationUri)
                .clientSecret(clientSecret)
                .tokenUri(accessTokenUri)
                .redirectUriTemplate(redirectUriTemplate)
                .scope(scope.split(","))
                .clientName(clientName)
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .userInfoUri(userInfoUri)
                .userNameAttributeName(userNameAttributeName)
                .jwkSetUri(jwkSetUri)
                .clientAuthenticationMethod(new ClientAuthenticationMethod(clientAuthenticationMethod))
                .build();
        return new InMemoryClientRegistrationRepository(Collections.singletonList(registration));
    }
}