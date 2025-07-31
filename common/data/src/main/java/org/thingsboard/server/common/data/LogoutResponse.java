/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LogoutResponse {
    @Schema(description = "The logout endpoint that logs out the authenticated user from SSO. Available if user logged in via oAuth2.", accessMode = Schema.AccessMode.READ_ONLY)
    private String endSessionEndpoint;

    @Schema(description = "The URI where End-User's User Agent will be redirected after a logout has been performed.", accessMode = Schema.AccessMode.READ_ONLY)
    private String redirectUri;

    public LogoutResponse() {

    }

    public LogoutResponse(String endSessionEndpoint, String redirectUri) {
        this.endSessionEndpoint = endSessionEndpoint;
        this.redirectUri = redirectUri;
    }
}