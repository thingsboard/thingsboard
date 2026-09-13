// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;

public class Oauth2AuthenticationSuccessHandlerTest {

    private static final String BASE_URL = "https://thingsboard.example.com";
    private static final String PREV_URI = "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e";
    private static final JwtPair TOKEN_PAIR = new JwtPair("testAccessToken", "testRefreshToken");

    private final JwtTokenFactory tokenFactory = mock(JwtTokenFactory.class);
    private final OAuth2ClientMapperProvider oauth2ClientMapperProvider = mock(OAuth2ClientMapperProvider.class);
    private final OAuth2ClientService oAuth2ClientService = mock(OAuth2ClientService.class);
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
    private final SystemSecurityService systemSecurityService = mock(SystemSecurityService.class);
    private final Oauth2AuthenticationSuccessHandler successHandler = new Oauth2AuthenticationSuccessHandler(
            tokenFactory, oauth2ClientMapperProvider, oAuth2ClientService, oAuth2AuthorizedClientService,
            mock(HttpCookieOAuth2AuthorizationRequestRepository.class), systemSecurityService);

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    public void before() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(systemSecurityService.getBaseUrl(any(TenantId.class), any(CustomerId.class), any(HttpServletRequest.class))).thenReturn(BASE_URL);
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testInAppPathIsTakenFromPrevUriCookie() {
        givenPrevUriCookie(PREV_URI + "?state=someState");
        assertThat(successHandler.getBaseUrl(request, null)).isEqualTo(BASE_URL);
        assertThat(successHandler.getPrevUri(request, response, null)).isEqualTo(PREV_URI + "?state=someState");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@evil.com/", "//evil.com", "https://evil.com", "/\\evil.com", "/dashboards#fragment"})
    public void testForgedPrevUriCookieIsIgnored(String prevUri) {
        givenPrevUriCookie(prevUri);
        assertThat(successHandler.getPrevUri(request, response, null)).isEmpty();
    }

    @Test
    public void testForgedPrevUriCookieIsDeleted() {
        givenPrevUriCookie("@evil.com/");

        successHandler.getPrevUri(request, response, null);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo(PREV_URI_COOKIE_NAME);
        assertThat(cookieCaptor.getValue().getMaxAge()).isZero();
    }

    @Test
    public void testBaseUrlWithoutPrevUriCookie() {
        when(request.getCookies()).thenReturn(null);
        assertThat(successHandler.getBaseUrl(request, null)).isEqualTo(BASE_URL);
        assertThat(successHandler.getPrevUri(request, response, null)).isEmpty();
    }

    @Test
    public void testCallbackUrlSchemeIgnoresPrevUri() {
        givenPrevUriCookie(PREV_URI);
        assertThat(successHandler.getBaseUrl(request, "tbmobile")).isEqualTo("tbmobile:");
        assertThat(successHandler.getPrevUri(request, response, "tbmobile")).isEmpty();
    }

    @Test
    public void testSuccessRedirectCarriesTokensToPrevUri() throws Exception {
        givenPrevUriCookie(PREV_URI);
        givenSuccessfulLogin();

        successHandler.onAuthenticationSuccess(request, response, givenAuthentication());

        assertThat(captureRedirect()).isEqualTo(BASE_URL + PREV_URI +
                "/?accessToken=testAccessToken&refreshToken=testRefreshToken");
    }

    /**
     * The error redirect appends its own path, so it must be built from the base URL alone - with prevUri in it the
     * result would be an unroutable https://host/dashboards/x/login?loginError=...
     */
    @Test
    public void testErrorRedirectDropsPrevUri() throws Exception {
        givenPrevUriCookie(PREV_URI);
        when(oAuth2ClientService.findOAuth2ClientById(any(), any())).thenThrow(new RuntimeException("someError"));

        successHandler.onAuthenticationSuccess(request, response, givenAuthentication());

        assertThat(captureRedirect()).isEqualTo(BASE_URL + "/login?loginError=someError");
    }

    @ParameterizedTest
    @CsvSource({
            "https://thingsboard.example.com/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e, https://thingsboard.example.com/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e/?",
            "https://thingsboard.example.com/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1, https://thingsboard.example.com/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1&",
            "https://thingsboard.example.com/, https://thingsboard.example.com/?"
    })
    public void testGetRedirectUrl(String baseUrl, String expectedPrefix) {
        assertThat(successHandler.getRedirectUrl(baseUrl, TOKEN_PAIR))
                .isEqualTo(expectedPrefix + "accessToken=testAccessToken&refreshToken=testRefreshToken");
    }

    private void givenPrevUriCookie(String prevUri) {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(PREV_URI_COOKIE_NAME, prevUri)});
    }

    private OAuth2AuthenticationToken givenAuthentication() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getName()).thenReturn("testUser");
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getAuthorizedClientRegistrationId()).thenReturn(UUID.randomUUID().toString());
        when(token.getPrincipal()).thenReturn(principal);
        return token;
    }

    private void givenSuccessfulLogin() {
        OAuth2Client oauth2Client = new OAuth2Client();
        oauth2Client.setMapperConfig(OAuth2MapperConfig.builder().type(MapperType.BASIC).build());
        when(oAuth2ClientService.findOAuth2ClientById(any(), any())).thenReturn(oauth2Client);

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "testProviderAccessToken", Instant.now(), Instant.now().plusSeconds(60)));
        when(oAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(authorizedClient);

        SecurityUser securityUser = mock(SecurityUser.class);
        OAuth2ClientMapper mapper = mock(OAuth2ClientMapper.class);
        when(mapper.getOrCreateUserByClientPrincipal(any(), any(), anyString(), any())).thenReturn(securityUser);
        when(oauth2ClientMapperProvider.getOAuth2ClientMapperByType(any())).thenReturn(mapper);
        when(tokenFactory.createTokenPair(securityUser)).thenReturn(TOKEN_PAIR);
    }

    private String captureRedirect() throws Exception {
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        return redirectCaptor.getValue();
    }

}
