// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
@TbCoreComponent
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String PREV_URI_PARAMETER = "prevUri";
    public static final String PREV_URI_COOKIE_NAME = "prev_uri";
    private static final int cookieExpireSeconds = 180;

    private static final byte[] KEY_DERIVATION_INFO = "thingsboard-oauth2-authorization-request-cookie".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_LENGTH = 32;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModules(SecurityJackson2Modules.getModules(HttpCookieOAuth2AuthorizationRequestRepository.class.getClassLoader()));

    private final JwtSettingsService jwtSettingsService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public HttpCookieOAuth2AuthorizationRequestRepository(JwtSettingsService jwtSettingsService) {
        this(jwtSettingsService, Clock.systemUTC());
    }

    HttpCookieOAuth2AuthorizationRequestRepository(JwtSettingsService jwtSettingsService, Clock clock) {
        this.jwtSettingsService = jwtSettingsService;
        this.clock = clock;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(this::decrypt)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }
        String prevUri = request.getParameter(PREV_URI_PARAMETER);
        if (PrevUriValidator.isValid(prevUri)) {
            CookieUtils.addCookie(response, PREV_URI_COOKIE_NAME, prevUri, cookieExpireSeconds);
        }
        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, encrypt(authorizationRequest), cookieExpireSeconds);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    }

    private String encrypt(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            byte[] json = OBJECT_MAPPER.writeValueAsBytes(authorizationRequest);
            byte[] plaintext = ByteBuffer.allocate(Long.BYTES + json.length).putLong(clock.millis()).put(json).array();
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            byte[] ciphertext = initCipher(Cipher.ENCRYPT_MODE, nonce).doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(NONCE_LENGTH + ciphertext.length).put(nonce).put(ciphertext).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt OAuth2 authorization request", e);
        }
    }

    private OAuth2AuthorizationRequest decrypt(Cookie cookie) {
        try {
            byte[] envelope = Base64.getUrlDecoder().decode(cookie.getValue());
            if (envelope.length < NONCE_LENGTH + TAG_LENGTH_BITS / Byte.SIZE + Long.BYTES) {
                log.debug("Ignoring OAuth2 authorization request cookie: too short");
                return null;
            }
            byte[] nonce = Arrays.copyOf(envelope, NONCE_LENGTH);
            byte[] plaintext = initCipher(Cipher.DECRYPT_MODE, nonce).doFinal(envelope, NONCE_LENGTH, envelope.length - NONCE_LENGTH);
            ByteBuffer buffer = ByteBuffer.wrap(plaintext);
            long issuedAt = buffer.getLong();
            if (clock.millis() - issuedAt > cookieExpireSeconds * 1000L) {
                log.debug("Ignoring OAuth2 authorization request cookie: expired");
                return null;
            }
            return OBJECT_MAPPER.readValue(plaintext, Long.BYTES, plaintext.length - Long.BYTES, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.debug("Ignoring invalid OAuth2 authorization request cookie: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private Cipher initCipher(int mode, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(deriveKey(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        return cipher;
    }

    private byte[] deriveKey() {
        byte[] signingKey = Base64.getDecoder().decode(jwtSettingsService.getJwtSettings().getTokenSigningKey());
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(signingKey, null, KEY_DERIVATION_INFO));
        byte[] key = new byte[KEY_LENGTH];
        hkdf.generateBytes(key, 0, KEY_LENGTH);
        return key;
    }

}
