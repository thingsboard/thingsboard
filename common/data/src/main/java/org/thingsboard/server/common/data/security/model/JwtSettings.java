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
package org.thingsboard.server.common.data.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "JWT Settings")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JwtSettings {

    /**
     * {@link JwtToken} will expire after this time.
     */
    @Schema(description = "The JWT will expire after seconds.", example = "9000")
    private Integer tokenExpirationTime;

    /**
     * {@link JwtToken} can be refreshed during this timeframe.
     */
    @Schema(description = "The JWT can be refreshed during seconds.", example = "604800")
    private Integer refreshTokenExpTime;

    /**
     * Token issuer.
     */
    @Schema(description = "The JWT issuer.", example = "thingsboard.io")
    private String tokenIssuer;

    /**
     * Key is used to sign {@link JwtToken}.
     * Base64 encoded
     */
    @Schema(description = "The JWT key is used to sing token. Base64 encoded.", example = "dkVTUzU2M2VMWUNwVVltTUhQU2o5SUM0Tkc3M0k2Ykdwcm85QTl6R0RaQ252OFlmVDk2OEptZXBNcndGeExFZg==")
    private String tokenSigningKey;

}
