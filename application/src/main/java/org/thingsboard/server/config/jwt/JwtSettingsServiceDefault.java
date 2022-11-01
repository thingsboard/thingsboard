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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtSettingsServiceDefault implements JwtSettingsService {

    static final String ADMIN_SETTINGS_JWT_KEY = "jwt";
    static final String TOKEN_SIGNING_KEY_DEFAULT = "thingsboardDefaultSigningKey";
    static final String TB_ALLOW_DEFAULT_JWT_SIGNING_KEY = "TB_ALLOW_DEFAULT_JWT_SIGNING_KEY";

    private final AdminSettingsService adminSettingsService;
    private final TbClusterService tbClusterService;

    private final JwtSettingsValidator jwtSettingsValidator;

    @Getter
    private final JwtSettings jwtSettings;

    @PostConstruct
    public void init() {
        reloadJwtSettings();
    }

    void reloadJwtSettings() {
        AdminSettings adminJwtSettings = findJwtAdminSettings();
        if (adminJwtSettings != null) {
            log.debug("Loading the JWT admin settings from database");
            JwtSettings jwtLoaded = mapAdminToJwtSettings(adminJwtSettings);
            jwtSettings.setRefreshTokenExpTime(jwtLoaded.getRefreshTokenExpTime());
            jwtSettings.setTokenExpirationTime(jwtLoaded.getTokenExpirationTime());
            jwtSettings.setTokenIssuer(jwtLoaded.getTokenIssuer());
            jwtSettings.setTokenSigningKey(jwtLoaded.getTokenSigningKey());
        }

        if (hasDefaultTokenSigningKey()) {
            log.warn("JWT token signing key is default. This is a security issue. Please, consider to set unique value");
        }
    }

    JwtSettings mapAdminToJwtSettings(AdminSettings adminSettings) {
        Objects.requireNonNull(adminSettings, "adminSettings for JWT is null");
        return JacksonUtil.treeToValue(adminSettings.getJsonValue(), JwtSettings.class);
    }

    AdminSettings mapJwtToAdminSettings(JwtSettings jwtSettings) {
        Objects.requireNonNull(jwtSettings, "jwtSettings is null");
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminJwtSettings.setKey(ADMIN_SETTINGS_JWT_KEY);
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(jwtSettings));
        return adminJwtSettings;
    }

    boolean hasDefaultTokenSigningKey() {
        return TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey());
    }

    @Override
    public void createJwtAdminSettings() {
        log.debug("Creating JWT admin settings...");
        Objects.requireNonNull(jwtSettings, "JWT settings is null");
        if (isJwtAdminSettingsNotExists()) {
            if (hasDefaultTokenSigningKey()) {
                if (!isAllowedDefaultJwtSigningKey()) {
                    log.info("JWT token signing key is default. Generating a new random key");
                    jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                            RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
                }
            }
            saveJwtSettings(jwtSettings);
        }
    }

    @Override
    public JwtSettings saveJwtSettings(JwtSettings jwtSettings){
        jwtSettingsValidator.validate(jwtSettings);
        AdminSettings adminJwtSettings = mapJwtToAdminSettings(jwtSettings);
        log.info("Saving new JWT admin settings. From this moment, the JWT parameters from YAML and ENV will be ignored");
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminJwtSettings);
        tbClusterService.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED);
        reloadJwtSettings();
        return getJwtSettings();
    }

    boolean isJwtAdminSettingsNotExists() {
        return findJwtAdminSettings() == null;
    }

    AdminSettings findJwtAdminSettings() {
        try {
            return adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        } catch (InvalidDataAccessResourceUsageException ignored) {
            log.debug("findAdminSettingsByKey is returning InvalidDataAccessResourceUsageException. This is an installation case when the database is not initialized yet");
            return null;
        }
    }

    /*
     * Allowing default JWT signing key is not secure
     * */
    boolean isAllowedDefaultJwtSigningKey() {
        String allowDefaultJwtSigningKey = System.getenv(TB_ALLOW_DEFAULT_JWT_SIGNING_KEY);
        return "true".equalsIgnoreCase(allowDefaultJwtSigningKey);
    }

    @Override
    public void validateJwtTokenSigningKey() {
        if (isJwtAdminSettingsNotExists() && hasDefaultTokenSigningKey()) {
            if (isAllowedDefaultJwtSigningKey()) {
                log.warn("Default JWT signing key is allowed. This is a security issue. Please, consider to set a strong key in admin settings");
            } else {
                String message = "Please, set a unique signing key with env variable JWT_TOKEN_SIGNING_KEY. Key is a Base64 encoded phrase. This will require to generate new tokens for all users and API that uses JWT tokens. To allow insecure JWS use TB_ALLOW_DEFAULT_JWT_SIGNING_KEY=true";
                log.error(message);
                throw new ValidationException(message);
            }
        }
    }

}
