// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;

@ExtendWith(MockitoExtension.class)
public class AdminControllerMailOAuth2Test {

    private static final String BASE_URL = "https://thingsboard.example.com";
    private static final String DEFAULT_PREV_URI = "/settings/outgoing-mail";

    @Mock
    private SystemSecurityService systemSecurityService;

    @InjectMocks
    private AdminController adminController;

    private HttpServletRequest request;

    @BeforeEach
    public void before() {
        request = mock(HttpServletRequest.class);
        when(systemSecurityService.getBaseUrl(any(TenantId.class), any(CustomerId.class), any(HttpServletRequest.class))).thenReturn(BASE_URL);
    }

    @Test
    public void testInAppPathIsTakenFromPrevUriCookie() {
        givenPrevUriCookie("/settings/notifications?tab=1");
        assertThat(adminController.getMailOAuth2RedirectUrl(request)).isEqualTo(BASE_URL + "/settings/notifications?tab=1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@evil.com/", "//evil.com", "https://evil.com", "/\\evil.com", "/settings#fragment"})
    public void testForgedPrevUriCookieIsIgnored(String prevUri) {
        givenPrevUriCookie(prevUri);
        assertThat(adminController.getMailOAuth2RedirectUrl(request)).isEqualTo(BASE_URL + DEFAULT_PREV_URI);
    }

    @Test
    public void testRedirectUrlWithoutPrevUriCookie() {
        when(request.getCookies()).thenReturn(null);
        assertThat(adminController.getMailOAuth2RedirectUrl(request)).isEqualTo(BASE_URL + DEFAULT_PREV_URI);
    }

    private void givenPrevUriCookie(String prevUri) {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(PREV_URI_COOKIE_NAME, prevUri)});
    }

}
