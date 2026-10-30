// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.security.model.token.OAuth2AppTokenFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AuthorizationRequestResolverTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;
    @Mock
    private OAuth2ClientService oAuth2ClientService;
    @Mock
    private OAuth2AppTokenFactory oAuth2AppTokenFactory;
    @InjectMocks
    private CustomOAuth2AuthorizationRequestResolver resolver;

    @Test
    void resolvesTheRegistrationIdFromTheAuthorizationPath() {
        given(clientRegistrationRepository.findByRegistrationId("abc-123")).willReturn(ClientRegistration.withRegistrationId("abc-123")
                .clientId("client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/")
                .authorizationUri("https://idp.example.com/authorize")
                .tokenUri("https://idp.example.com/token")
                .build());

        OAuth2AuthorizationRequest request = resolver.resolve(new MockHttpServletRequest("GET", "/oauth2/authorization/abc-123"));

        assertThat(request.getAttributes()).containsEntry(OAuth2ParameterNames.REGISTRATION_ID, "abc-123");
        assertThat(request.getAuthorizationUri()).isEqualTo("https://idp.example.com/authorize");
    }

    @Test
    void ignoresDeeperPaths() {
        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/oauth2/authorization/abc/extra"))).isNull();
        verifyNoInteractions(clientRegistrationRepository);
    }

}
