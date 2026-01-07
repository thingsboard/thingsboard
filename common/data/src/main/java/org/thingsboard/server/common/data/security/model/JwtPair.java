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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.security.Authority;

import java.io.Serializable;

@Schema(description = "JWT Pair")
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtPair implements Serializable {

    @Schema(description = "The JWT Access Token. Used to perform API calls.", example = "AAB254FF67D..")
    private String token;
    @Schema(description = "The JWT Refresh Token. Used to get new JWT Access Token if old one has expired.", example = "AAB254FF67D..")
    private String refreshToken;

    private Authority scope;

    public JwtPair(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

}
