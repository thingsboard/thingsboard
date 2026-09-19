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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@TbCoreComponent
public class DingTalkOAuth2UserService extends DefaultOAuth2UserService {

    private static final String DINGTALK_USER_INFO_URI = "https://api.dingtalk.com/v1.0/contact/users/me";

    private final RestTemplate restTemplate;

    public DingTalkOAuth2UserService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        ClientRegistration.ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
        String userInfoUri = providerDetails.getUserInfoEndpoint().getUri();

        if (!DINGTALK_USER_INFO_URI.equals(userInfoUri)) {
            return super.loadUser(userRequest);
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<?> entity = new HttpEntity<>(headers);

        Map<String, Object> responseBody;
        try {
            responseBody = restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, Map.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Failed to get user info from DingTalk: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuth2AuthenticationException("Failed to get user info from DingTalk: HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to get user info from DingTalk: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Failed to get user info from DingTalk: " + e.getMessage());
        }

        if (responseBody == null) {
            throw new OAuth2AuthenticationException("Empty response from DingTalk user info endpoint");
        }

        Map<String, Object> userAttributes = new HashMap<>(responseBody);

        String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();

        if (userAttributes.get(userNameAttributeName) == null) {
            if (userAttributes.get("unionId") != null) {
                userAttributes.put(userNameAttributeName, userAttributes.get("unionId"));
                log.warn("DingTalk response missing '{}', using 'unionId' as fallback", userNameAttributeName);
            } else if (userAttributes.get("openContactId") != null) {
                userAttributes.put(userNameAttributeName, userAttributes.get("openContactId"));
                log.warn("DingTalk response missing '{}', using 'openContactId' as fallback", userNameAttributeName);
            } else {
                log.error("DingTalk response missing '{}' and no fallback fields available. Response: {}", userNameAttributeName, responseBody);
                throw new OAuth2AuthenticationException("DingTalk response does not contain '" + userNameAttributeName + "' or any fallback identifier (unionId, openContactId)");
            }
        }

        return new DefaultOAuth2User(
                Collections.emptyList(),
                userAttributes,
                userNameAttributeName
        );
    }
}
