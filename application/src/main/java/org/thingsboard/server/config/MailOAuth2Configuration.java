/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "mail.oauth2")
@Data
public class MailOAuth2Configuration {

    private List<MailOauth2ProviderConfiguration> providers;

    public MailOauth2ProviderConfiguration getProviderConfig(MailOauth2Provider mailOauth2Provider) {
        return providers.stream().filter(conf -> conf.getName().equals(mailOauth2Provider.name()))
                .findFirst().orElse(null);
    }
}
