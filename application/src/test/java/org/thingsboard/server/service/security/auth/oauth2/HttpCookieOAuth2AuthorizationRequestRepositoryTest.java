// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_PARAMETER;

public class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private final HttpCookieOAuth2AuthorizationRequestRepository repository = new HttpCookieOAuth2AuthorizationRequestRepository();

    @Test
    public void testPrevUriSavedForInAppPath() {
        assertThat(savePrevUri("/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState"))
                .isEqualTo("/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"@evil.com/"})
    public void testPrevUriNotSavedForInvalidValue(String prevUri) {
        assertThat(savePrevUri(prevUri)).isNull();
    }

    private String savePrevUri(String prevUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter(PREV_URI_PARAMETER)).thenReturn(prevUri);

        repository.saveAuthorizationRequest(OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("testUri").clientId("testId").build(), request, response);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeastOnce()).addCookie(cookieCaptor.capture());
        return cookieCaptor.getAllValues().stream()
                .filter(cookie -> PREV_URI_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findAny().orElse(null);
    }

}
