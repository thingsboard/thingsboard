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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@TbCoreComponent
public class DingTalkTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private static final String DINGTALK_TOKEN_URI = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final long DEFAULT_EXPIRE_SECONDS = 7200;

    private final RestTemplate restTemplate;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> defaultClient;

    public DingTalkTokenResponseClient() {
        this.restTemplate = new RestTemplate();
        this.defaultClient = new DefaultAuthorizationCodeTokenResponseClient();
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        if (supports(grantRequest.getClientRegistration())) {
            return getTokenResponseInternal(grantRequest);
        }
        return defaultClient.getTokenResponse(grantRequest);
    }

    private OAuth2AccessTokenResponse getTokenResponseInternal(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        ClientRegistration clientRegistration = grantRequest.getClientRegistration();
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();

        Map<String, String> body = new HashMap<>();
        body.put("clientId", clientRegistration.getClientId());
        body.put("clientSecret", clientRegistration.getClientSecret());
        body.put("code", grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
        body.put("grantType", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> responseBody;
        try {
            responseBody = restTemplate.exchange(tokenUri, HttpMethod.POST, entity, Map.class).getBody();
        } catch (Exception e) {
            log.error("Failed to get access token from DingTalk: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Failed to get access token from DingTalk: " + e.getMessage());
        }

        if (responseBody == null) {
            throw new OAuth2AuthenticationException("Empty response from DingTalk token endpoint");
        }

        if (responseBody.containsKey("code")) {
            String responseCode = String.valueOf(responseBody.get("code"));
            if (!"0".equals(responseCode)) {
                String message = responseBody.containsKey("message") ? String.valueOf(responseBody.get("message")) : "Unknown error";
                throw new OAuth2AuthenticationException("DingTalk error [" + responseCode + "]: " + message);
            }
        }

        Object accessToken = responseBody.get("accessToken");
        if (accessToken == null) {
            throw new OAuth2AuthenticationException("DingTalk token response missing 'accessToken'");
        }

        String refreshToken = responseBody.containsKey("refreshToken") ? String.valueOf(responseBody.get("refreshToken")) : null;
        long expireIn = DEFAULT_EXPIRE_SECONDS;
        if (responseBody.containsKey("expireIn")) {
            try {
                expireIn = Long.parseLong(String.valueOf(responseBody.get("expireIn")));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse expireIn from DingTalk response, using default: {}", DEFAULT_EXPIRE_SECONDS);
            }
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        if (responseBody.containsKey("corpId")) {
            additionalParameters.put("corpId", responseBody.get("corpId"));
        }

        if (!CollectionUtils.isEmpty(additionalParameters)) {
            return OAuth2AccessTokenResponse.withToken(String.valueOf(accessToken))
                    .tokenType(OAuth2AccessToken.TokenType.BEARER)
                    .expiresIn(expireIn)
                    .refreshToken(refreshToken)
                    .scopes(grantRequest.getAuthorizationExchange().getAuthorizationRequest().getScopes())
                    .additionalParameters(additionalParameters)
                    .build();
        }

        return OAuth2AccessTokenResponse.withToken(String.valueOf(accessToken))
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(expireIn)
                .refreshToken(refreshToken)
                .scopes(grantRequest.getAuthorizationExchange().getAuthorizationRequest().getScopes())
                .build();
    }

    public boolean supports(ClientRegistration clientRegistration) {
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        return DINGTALK_TOKEN_URI.equals(tokenUri);
    }
}
