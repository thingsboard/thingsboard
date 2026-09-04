// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.oauth2;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class OAuth2ClientLoginInfo {

    @Schema(description = "OAuth2 client name", example = "GitHub")
    private String name;
    @Schema(description = "Name of the icon, displayed on OAuth2 log in button", example = "github-logo")
    private String icon;
    @Schema(description = "URI for OAuth2 log in. On HTTP GET request to this URI, it redirects to the OAuth2 provider page",
            example = "/oauth2/authorization/8352f191-2b4d-11ec-9ed1-cbf57c026ecc")
    private String url;

}
