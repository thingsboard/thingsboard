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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.MapperType;

@Component
@Slf4j
public class OAuth2ClientMapperProvider {

    @Autowired
    @Qualifier("basicOAuth2ClientMapper")
    private OAuth2ClientMapper basicOAuth2ClientMapper;

    @Autowired
    @Qualifier("customOAuth2ClientMapper")
    private OAuth2ClientMapper customOAuth2ClientMapper;

    @Autowired
    @Qualifier("githubOAuth2ClientMapper")
    private OAuth2ClientMapper githubOAuth2ClientMapper;

    public OAuth2ClientMapper getOAuth2ClientMapperByType(MapperType oauth2MapperType) {
        switch (oauth2MapperType) {
            case CUSTOM:
                return customOAuth2ClientMapper;
            case BASIC:
                return basicOAuth2ClientMapper;
            case GITHUB:
                return githubOAuth2ClientMapper;
            default:
                throw new RuntimeException("OAuth2ClientRegistrationMapper with type " + oauth2MapperType + " is not supported!");
        }
    }
}
