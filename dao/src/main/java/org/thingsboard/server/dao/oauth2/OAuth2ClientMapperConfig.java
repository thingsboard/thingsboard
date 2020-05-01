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
package org.thingsboard.server.dao.oauth2;

import lombok.Data;

@Data
public class OAuth2ClientMapperConfig {

    private String type;
    private BasicOAuth2ClientMapperConfig basic;
    private CustomOAuth2ClientMapperConfig custom;

    @Data
    public static class BasicOAuth2ClientMapperConfig {
        private boolean allowUserCreation;
        private String emailAttributeKey;
        private String firstNameAttributeKey;
        private String lastNameAttributeKey;
        private String tenantNameStrategy;
        private String tenantNameStrategyPattern;
        private String customerNameStrategyPattern;
    }

    @Data
    public static class CustomOAuth2ClientMapperConfig {
        private String url;
        private String username;
        private String password;
    }
}
