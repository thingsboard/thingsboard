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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService.isSigningKeyDefault;
import static org.thingsboard.server.service.security.model.token.JwtTokenFactory.KEY_LENGTH;

@Component
@RequiredArgsConstructor
public class DefaultJwtSettingsValidator implements JwtSettingsValidator {

    @Override
    public void validate(JwtSettings jwtSettings) {
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(15)) {
            throw new DataValidationException("JWT refresh token expiration time should be at least 15 minutes!");
        }
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(1)) {
            throw new DataValidationException("JWT token expiration time should be at least 1 minute!");
        }
        if (jwtSettings.getTokenExpirationTime() >= jwtSettings.getRefreshTokenExpTime()) {
            throw new DataValidationException("JWT token expiration time should greater than JWT refresh token expiration time!");
        }
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be a valid Base64 encoded string! " + e.getMessage());
        }

        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        if (decodedKey.length * Byte.SIZE < KEY_LENGTH && !isSigningKeyDefault(jwtSettings)) {
            throw new DataValidationException("JWT token signing key should be a Base64 encoded string representing at least 512 bits of data!");
        }

        System.arraycopy(decodedKey, 0, RandomUtils.secure().randomBytes(decodedKey.length), 0, decodedKey.length); // secure memory
    }

}
