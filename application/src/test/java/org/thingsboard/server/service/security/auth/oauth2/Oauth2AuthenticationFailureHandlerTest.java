// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Oauth2AuthenticationFailureHandlerTest {

    private static final String BASE_URL = "https://thingsboard.example.com";

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository =
            mock(HttpCookieOAuth2AuthorizationRequestRepository.class);
    private final SystemSecurityService systemSecurityService = mock(SystemSecurityService.class);
    private final Oauth2AuthenticationFailureHandler failureHandler =
            new Oauth2AuthenticationFailureHandler(authorizationRequestRepository, systemSecurityService);

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    public void before() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSecurityService.getBaseUrl(any(TenantId.class), any(CustomerId.class), any(HttpServletRequest.class))).thenReturn(BASE_URL);
    }

    @Test
    public void testErrorIsSentToMobileAppScheme() throws Exception {
        givenCallbackUrlScheme("tbmobile");
        assertThat(sendFailure()).isEqualTo("tbmobile:/?error=someError");
    }

    /**
     * The scheme is restored from the oauth2_auth_request cookie, so a forged one must not turn the error redirect
     * into a link to another host.
     */
    @ParameterizedTest
    @ValueSource(strings = {"https://evil.com", "javascript"})
    public void testForgedCallbackUrlSchemeFallsBackToLoginPage(String callbackUrlScheme) throws Exception {
        givenCallbackUrlScheme(callbackUrlScheme);
        assertThat(sendFailure()).isEqualTo(BASE_URL + "/login?loginError=someError");
    }

    @Test
    public void testErrorIsSentToLoginPageWithoutCallbackUrlScheme() throws Exception {
        givenCallbackUrlScheme(null);
        assertThat(sendFailure()).isEqualTo(BASE_URL + "/login?loginError=someError");
    }

    @Test
    public void testErrorIsSentToLoginPageWithoutAuthorizationRequest() throws Exception {
        assertThat(sendFailure()).isEqualTo(BASE_URL + "/login?loginError=someError");
    }

    private void givenCallbackUrlScheme(String callbackUrlScheme) {
        when(authorizationRequestRepository.loadAuthorizationRequest(request)).thenReturn(
                OAuth2AuthorizationRequest.authorizationCode().authorizationUri("testUri").clientId("testId")
                        .attributes(attributes -> attributes.put(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME, callbackUrlScheme))
                        .build());
    }

    private String sendFailure() throws Exception {
        failureHandler.onAuthenticationFailure(request, response, new AuthenticationServiceException("someError"));

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        return redirectCaptor.getValue();
    }

}
