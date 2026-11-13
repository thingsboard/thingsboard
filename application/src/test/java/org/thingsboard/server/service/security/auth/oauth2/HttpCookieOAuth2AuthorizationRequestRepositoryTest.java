// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_PARAMETER;

public class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final long NOW = 1_700_000_000_000L;

    private final JwtSettingsService jwtSettingsService = mock(JwtSettingsService.class);
    private final Clock clock = mock(Clock.class);
    private final HttpCookieOAuth2AuthorizationRequestRepository repository =
            new HttpCookieOAuth2AuthorizationRequestRepository(jwtSettingsService, clock);

    @BeforeEach
    public void before() {
        givenSigningKey(randomSigningKey());
        when(clock.millis()).thenReturn(NOW);
    }

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

    @Test
    public void testAuthorizationRequestRoundTrip() {
        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        additionalParameters.put("param1", "value1");
        additionalParameters.put("param2", "value2");
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("testUri").clientId("testId")
                .scope("read", "write")
                .state("testState")
                .additionalParameters(additionalParameters)
                .attributes(attributes -> attributes.put(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME, "tbmobile"))
                .build();

        OAuth2AuthorizationRequest loaded = load(save(authorizationRequest));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getGrantType()).isEqualTo(authorizationRequest.getGrantType());
        assertThat(loaded.getAuthorizationUri()).isEqualTo(authorizationRequest.getAuthorizationUri());
        assertThat(loaded.getClientId()).isEqualTo(authorizationRequest.getClientId());
        assertThat(loaded.getScopes()).isEqualTo(authorizationRequest.getScopes());
        assertThat(loaded.getState()).isEqualTo(authorizationRequest.getState());
        assertThat(loaded.getAdditionalParameters()).isEqualTo(additionalParameters);
        assertThat(CallbackUrlSchemeValidator.getCallbackUrlScheme(loaded)).isEqualTo("tbmobile");
    }

    @Test
    public void testCookieIsNotPlainJson() {
        String cookieValue = save(authorizationRequest());
        assertThat(new String(Base64.getUrlDecoder().decode(cookieValue))).doesNotContain("testId");
    }

    @Test
    public void testEachCookieUsesFreshNonce() {
        assertThat(save(authorizationRequest())).isNotEqualTo(save(authorizationRequest()));
    }

    @Test
    public void testTamperedCookieIsRejected() {
        byte[] envelope = Base64.getUrlDecoder().decode(save(authorizationRequest()));
        for (int i = 0; i < envelope.length; i++) {
            byte[] tampered = envelope.clone();
            tampered[i] ^= 0x01;
            assertThat(load(Base64.getUrlEncoder().withoutPadding().encodeToString(tampered))).as("bit flipped at byte %s", i).isNull();
        }
    }

    @Test
    public void testTruncatedCookieIsRejected() {
        String cookieValue = save(authorizationRequest());
        assertThat(load(cookieValue.substring(0, cookieValue.length() - 4))).isNull();
    }

    @Test
    public void testCookieEncryptedWithDifferentKeyIsRejected() {
        String cookieValue = save(authorizationRequest());
        givenSigningKey(randomSigningKey());
        assertThat(load(cookieValue)).isNull();
    }

    @Test
    public void testNewCookieIsAcceptedAfterKeyRotation() {
        givenSigningKey(randomSigningKey());
        String cookieValue = save(authorizationRequest());
        assertThat(load(cookieValue)).isNotNull();
    }

    @Test
    public void testUnencryptedCookieIsRejected() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        String plainCookieValue = Base64.getUrlEncoder().encodeToString(mapper.writeValueAsBytes(authorizationRequest()));
        assertThat(load(plainCookieValue)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "AAAA", "not base64 !", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    public void testMalformedCookieIsRejected(String cookieValue) {
        assertThat(load(cookieValue)).isNull();
    }

    @Test
    public void testExpiredCookieIsRejected() {
        String cookieValue = save(authorizationRequest());
        when(clock.millis()).thenReturn(NOW + 180_000);
        assertThat(load(cookieValue)).isNotNull();
        when(clock.millis()).thenReturn(NOW + 180_001);
        assertThat(load(cookieValue)).isNull();
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode().authorizationUri("testUri").clientId("testId").build();
    }

    private String save(OAuth2AuthorizationRequest authorizationRequest) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        repository.saveAuthorizationRequest(authorizationRequest, request, response);
        return findCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    }

    private OAuth2AuthorizationRequest load(String cookieValue) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, cookieValue)});
        return repository.loadAuthorizationRequest(request);
    }

    private String savePrevUri(String prevUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter(PREV_URI_PARAMETER)).thenReturn(prevUri);

        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        return findCookie(response, PREV_URI_COOKIE_NAME);
    }

    private String findCookie(HttpServletResponse response, String name) {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeastOnce()).addCookie(cookieCaptor.capture());
        return cookieCaptor.getAllValues().stream()
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findAny().orElse(null);
    }

    private void givenSigningKey(String signingKey) {
        JwtSettings jwtSettings = new JwtSettings();
        jwtSettings.setTokenSigningKey(signingKey);
        when(jwtSettingsService.getJwtSettings()).thenReturn(jwtSettings);
    }

    private static String randomSigningKey() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

}
