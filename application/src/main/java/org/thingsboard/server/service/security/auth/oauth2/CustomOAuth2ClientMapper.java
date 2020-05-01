/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.dao.oauth2.OAuth2ClientMapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service(value = "customOAuth2ClientMapper")
@Slf4j
public class CustomOAuth2ClientMapper extends BaseOAuth2ClientMapper implements OAuth2ClientMapper {

    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(OAuth2AuthenticationToken token, OAuth2ClientMapperConfig config) {
        OAuth2User oauth2User = getOAuth2User(token, config.getCustom());
        return getOrCreateSecurityUserFromOAuth2User(oauth2User, config.getBasic().isAllowUserCreation());
    }

    public OAuth2User getOAuth2User(OAuth2AuthenticationToken token, OAuth2ClientMapperConfig.CustomOAuth2ClientMapperConfig custom) {
        if (!StringUtils.isEmpty(custom.getUsername()) && !StringUtils.isEmpty(custom.getPassword())) {
            restTemplateBuilder = restTemplateBuilder.basicAuthentication(custom.getUsername(), custom.getPassword());
        }
        RestTemplate restTemplate = restTemplateBuilder.build();
        return restTemplate.postForEntity(custom.getUrl(), token.getPrincipal(), OAuth2User.class).getBody();
    }
}
