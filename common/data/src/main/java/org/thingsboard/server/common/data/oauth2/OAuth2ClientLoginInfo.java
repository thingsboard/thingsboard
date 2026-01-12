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
