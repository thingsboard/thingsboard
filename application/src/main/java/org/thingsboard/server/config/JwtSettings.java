/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtToken;

@Component
@ConfigurationProperties(prefix = "security.jwt")
@Data
public class JwtSettings {
    /**
     * {@link JwtToken} will expire after this time.
     */
    private Integer tokenExpirationTime;

    /**
     * Token issuer.
     */
    private String tokenIssuer;

    /**
     * Key is used to sign {@link JwtToken}.
     * Base64 encoded
     */
    private String tokenSigningKey;

    /**
     * {@link JwtToken} can be refreshed during this timeframe.
     */
    private Integer refreshTokenExpTime;

}
