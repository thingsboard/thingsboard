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
package org.thingsboard.server.service.security.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;

public class Oauth2AuthenticationSuccessHandlerBaseUrlTest {

    private static final String BASE_URL = "https://thingsboard.example.com";

    private final SystemSecurityService systemSecurityService = mock(SystemSecurityService.class);
    private final Oauth2AuthenticationSuccessHandler successHandler = new Oauth2AuthenticationSuccessHandler(
            mock(JwtTokenFactory.class), mock(OAuth2ClientMapperProvider.class), mock(OAuth2ClientService.class),
            mock(OAuth2AuthorizedClientService.class), mock(HttpCookieOAuth2AuthorizationRequestRepository.class),
            systemSecurityService);

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    public void before() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(systemSecurityService.getBaseUrl(any(TenantId.class), any(CustomerId.class), any(HttpServletRequest.class))).thenReturn(BASE_URL);
    }

    @Test
    public void testInAppPathIsAppendedToBaseUrl() {
        givenPrevUriCookie("/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState");
        assertThat(successHandler.getBaseUrl(request, response, null))
                .isEqualTo(BASE_URL + "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@evil.com/", "//evil.com", "https://evil.com", "/\\evil.com", "/dashboards#fragment"})
    public void testForgedPrevUriCookieIsIgnored(String prevUri) {
        givenPrevUriCookie(prevUri);
        assertThat(successHandler.getBaseUrl(request, response, null)).isEqualTo(BASE_URL);
    }

    @Test
    public void testForgedPrevUriCookieIsDeleted() {
        givenPrevUriCookie("@evil.com/");

        successHandler.getBaseUrl(request, response, null);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo(PREV_URI_COOKIE_NAME);
        assertThat(cookieCaptor.getValue().getMaxAge()).isZero();
    }

    @Test
    public void testBaseUrlWithoutPrevUriCookie() {
        when(request.getCookies()).thenReturn(null);
        assertThat(successHandler.getBaseUrl(request, response, null)).isEqualTo(BASE_URL);
    }

    @Test
    public void testCallbackUrlSchemeIgnoresPrevUri() {
        givenPrevUriCookie("/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e");
        assertThat(successHandler.getBaseUrl(request, response, "tbmobile")).isEqualTo("tbmobile:");
    }

    private void givenPrevUriCookie(String prevUri) {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(PREV_URI_COOKIE_NAME, prevUri)});
    }

}
