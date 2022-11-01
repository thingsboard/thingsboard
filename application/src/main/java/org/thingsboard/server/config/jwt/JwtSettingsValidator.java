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
package org.thingsboard.server.config.jwt;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Base64;
import java.util.Optional;

@Component
@AllArgsConstructor
public class JwtSettingsValidator {

    public void validate(JwtSettings jwtSettings) {
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) <= 0) {
            throw new DataValidationException("JWT refresh token expiration time should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) <= 0) {
            throw new DataValidationException("JWT token expiration time should be specified!");
        }
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be valid Base64 encoded string! " + e.getCause());
        }

        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        Arrays.fill(decodedKey, (byte) 0);
    }

}
