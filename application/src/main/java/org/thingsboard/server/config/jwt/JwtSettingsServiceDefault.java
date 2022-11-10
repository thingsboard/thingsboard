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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtSettingsServiceDefault implements JwtSettingsService {

    static final String ADMIN_SETTINGS_JWT_KEY = "jwt";
    static final String TOKEN_SIGNING_KEY_DEFAULT = "thingsboardDefaultSigningKey";
    static final String TB_ALLOW_DEFAULT_JWT_SIGNING_KEY = "TB_ALLOW_DEFAULT_JWT_SIGNING_KEY";
    @Lazy
    private final AdminSettingsService adminSettingsService;
    @Lazy
    private final Optional<TbClusterService> tbClusterService;
    private final JwtSettingsValidator jwtSettingsValidator;
    private final Environment environment;
    @Getter
    private final JwtSettings jwtSettings;
    @Value("${install.upgrade:false}")
    private boolean isUpgrade;

    @PostConstruct
    public void init() {
        if (!isFirstInstall()) {
            reloadJwtSettings();
        }
    }

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    private boolean isFirstInstall() {
        return isInstall() && !isUpgrade;
    }

    @Override
    public void reloadJwtSettings() {
        AdminSettings adminJwtSettings = findJwtAdminSettings();
        if (adminJwtSettings != null) {
            log.info("Reloading the JWT admin settings from database");
            JwtSettings jwtLoaded = mapAdminToJwtSettings(adminJwtSettings);
            jwtSettings.setRefreshTokenExpTime(jwtLoaded.getRefreshTokenExpTime());
            jwtSettings.setTokenExpirationTime(jwtLoaded.getTokenExpirationTime());
            jwtSettings.setTokenIssuer(jwtLoaded.getTokenIssuer());
            jwtSettings.setTokenSigningKey(jwtLoaded.getTokenSigningKey());
        }

        if (hasDefaultTokenSigningKey()) {
            log.warn("WARNING: The platform is configured to use default JWT Signing Key. " +
                    "This is a security issue that needs to be resolved. Please change the JWT Signing Key using the Web UI. " +
                    "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.");
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

    /**
     * Create JWT admin settings is intended to be called from Install or Upgrade scripts
     * */
    @Override
    public void createJwtAdminSettings() {
        log.info("Creating JWT admin settings...");
        Objects.requireNonNull(jwtSettings, "JWT settings is null");
        if (isJwtAdminSettingsNotExists()) {
            if (hasDefaultTokenSigningKey() && isFirstInstall()) {
                log.info("JWT token signing key is default. Generating a new random key");
                jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                        RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
            }
            saveJwtSettings(jwtSettings);
        }
    }

    @Override
    public JwtSettings saveJwtSettings(JwtSettings jwtSettings) {
        jwtSettingsValidator.validate(jwtSettings);
        final AdminSettings adminJwtSettings = mapJwtToAdminSettings(jwtSettings);
        final AdminSettings existedSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (existedSettings != null) {
            adminJwtSettings.setId(existedSettings.getId());
        }

        log.info("Saving new JWT admin settings. From this moment, the JWT parameters from YAML and ENV will be ignored");
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminJwtSettings);

        if (!isInstall()) {
            tbClusterService.orElseThrow().broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED);
        }
        reloadJwtSettings();
        return getJwtSettings();
    }

    boolean isJwtAdminSettingsNotExists() {
        return findJwtAdminSettings() == null;
    }

    AdminSettings findJwtAdminSettings() {
        return adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
    }

}
