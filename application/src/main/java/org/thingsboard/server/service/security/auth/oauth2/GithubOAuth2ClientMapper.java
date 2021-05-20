/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Service(value = "githubOAuth2ClientMapper")
@Slf4j
public class GithubOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {
    private static final String EMAIL_URL_KEY = "emailUrl";

    private static final String AUTHORIZATION = "Authorization";

    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @Autowired
    private OAuth2Configuration oAuth2Configuration;

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(OAuth2AuthenticationToken token, String providerAccessToken, OAuth2ClientRegistrationInfo clientRegistration) {
        OAuth2MapperConfig config = clientRegistration.getMapperConfig();
        Map<String, String> githubMapperConfig = oAuth2Configuration.getGithubMapper();
        String email = getEmail(githubMapperConfig.get(EMAIL_URL_KEY), providerAccessToken);
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        OAuth2User oAuth2User = BasicMapperUtils.getOAuth2User(email, attributes, config);
        return getOrCreateSecurityUserFromOAuth2User(oAuth2User, clientRegistration);
    }

    private synchronized String getEmail(String emailUrl, String oauth2Token) {
        restTemplateBuilder = restTemplateBuilder.defaultHeader(AUTHORIZATION, "token " + oauth2Token);

        RestTemplate restTemplate = restTemplateBuilder.build();
        GithubEmailsResponse githubEmailsResponse;
        try {
            githubEmailsResponse = restTemplate.getForEntity(emailUrl, GithubEmailsResponse.class).getBody();
            if (githubEmailsResponse == null){
                throw new RuntimeException("Empty Github response!");
            }
        } catch (Exception e) {
            log.error("There was an error during connection to Github API", e);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
        Optional<String> emailOpt = githubEmailsResponse.stream()
                .filter(GithubEmailResponse::isPrimary)
                .map(GithubEmailResponse::getEmail)
                .findAny();
        if (emailOpt.isPresent()){
            return emailOpt.get();
        } else {
            log.error("Could not find primary email from {}.", githubEmailsResponse);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
    }
    private static class GithubEmailsResponse extends ArrayList<GithubEmailResponse> {}

    @Data
    @ToString
    private static class GithubEmailResponse {
        private String email;
        private boolean verified;
        private boolean primary;
        private String visibility;
    }
}
