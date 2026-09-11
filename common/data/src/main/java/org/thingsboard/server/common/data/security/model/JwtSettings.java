// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
