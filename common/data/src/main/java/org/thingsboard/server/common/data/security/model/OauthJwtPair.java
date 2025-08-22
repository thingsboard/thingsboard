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
package org.thingsboard.server.common.data.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class OauthJwtPair extends JwtPair {
    @Schema(description = "The JWT oAuth 2.0 OpenID identity token. Used to confirms the identity of the user (i.e., who logged in) in oAuth 2.0 provider.", example = "AAB254FF67D..")
    private String idToken;

    public OauthJwtPair(String token, String refreshToken, String idToken) {
        super(token, refreshToken);
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }
}
