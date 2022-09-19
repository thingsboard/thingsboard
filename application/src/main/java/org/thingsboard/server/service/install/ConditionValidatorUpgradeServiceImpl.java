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
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.validation.ValidationException;

import static org.thingsboard.server.config.JwtSettings.ADMIN_SETTINGS_JWT_KEY;

@Service
@Profile("install")
@RequiredArgsConstructor
@Slf4j
public class ConditionValidatorUpgradeServiceImpl implements ConditionValidatorUpgradeService {

    private final AdminSettingsService adminSettingsService;

    private final JwtSettings jwtSettings;

    @Override
    public void validateConditionsBeforeUpgrade(String fromVersion) throws Exception {
        log.info("Validating conditions before upgrade..");
        validateJwtTokenSigningKey();
    }

    void validateJwtTokenSigningKey() {
        AdminSettings adminJwtSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (adminJwtSettings == null) {
            if (jwtSettings.hasDefaultTokenSigningKey()) {
                String allowDefaultJwtSigningKey = System.getenv("TB_ALLOW_DEFAULT_JWT_SIGNING_KEY");
                if ("true".equalsIgnoreCase(allowDefaultJwtSigningKey)) {
                    log.warn("Default JWT signing key is allowed. This is a security issue. Please, consider to set a strong key in admin settings");
                } else {
                    String message = "Please, set a unique signing key with env variable JWT_TOKEN_SIGNING_KEY. Key is a Base64 encoded phrase. This will require to generate new tokens for all users and API that uses JWT tokens. To allow insecure JWS use TB_ALLOW_DEFAULT_JWT_SIGNING_KEY=true";
                    log.error(message);
                    throw new ValidationException(message);
                }
            }
        }
    }

}
