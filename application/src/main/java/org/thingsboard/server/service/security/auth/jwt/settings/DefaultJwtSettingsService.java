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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsValidator.ADMIN_SETTINGS_JWT_KEY;
import static org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsValidator.TOKEN_SIGNING_KEY_DEFAULT;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJwtSettingsService implements JwtSettingsService {

    @Lazy
    private final AdminSettingsService adminSettingsService;
    @Lazy
    private final Optional<TbClusterService> tbClusterService;
    private final JwtSettingsValidator jwtSettingsValidator;
    private volatile JwtSettings jwtSettings = null; //lazy init
    @Value("${install.upgrade:false}")
    private boolean isUpgrade;

    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;
    @Value("${security.jwt.tokenSigningKey:thingsboardDefaultSigningKey}")
    private String tokenSigningKey;

    @PostConstruct
    public void init() {

    }

    @Override
    public void reloadJwtSettings() {
        AdminSettings adminJwtSettings = findJwtAdminSettings();
        if (adminJwtSettings != null) {
            log.info("Reloading the JWT admin settings from database");
            synchronized (this) {
                this.jwtSettings = mapAdminToJwtSettings(adminJwtSettings);
            }
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
        return TOKEN_SIGNING_KEY_DEFAULT.equals(getJwtSettings().getTokenSigningKey());
    }

    /**
     * Create JWT admin settings is intended to be called from Install scripts only
     * */
    @Override
    public void createRandomJwtSettings() {
        log.info("Creating JWT admin settings...");
        Objects.requireNonNull(getJwtSettings(), "JWT settings is null");

        if (hasDefaultTokenSigningKey()) {
            log.info("JWT token signing key is default. Generating a new random key");
            getJwtSettings().setTokenSigningKey(Base64.getEncoder().encodeToString(
                    RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8)));
        }
        saveJwtSettings(getJwtSettings());
    }

    /**
     * Create JWT admin settings is intended to be called from Upgrade scripts only
     * */
    @Override
    public void saveLegacyYmlSettings() {
        log.info("Saving legacy JWT admin settings from YML...");
        Objects.requireNonNull(getJwtSettings(), "JWT settings is null");
        if (isJwtAdminSettingsNotExists()) {
            saveJwtSettings(getJwtSettings());
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

        tbClusterService.ifPresent(cs -> cs.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED));
        reloadJwtSettings();
        return getJwtSettings();
    }

    boolean isJwtAdminSettingsNotExists() {
        return findJwtAdminSettings() == null;
    }

    AdminSettings findJwtAdminSettings() {
        return adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
    }

    public JwtSettings getJwtSettings() {
        if (this.jwtSettings == null) {
            synchronized (this) {
                if (this.jwtSettings == null) {
                    this.jwtSettings = new JwtSettings(this.tokenExpirationTime, this.refreshTokenExpTime, this.tokenIssuer, this.tokenSigningKey);
                    reloadJwtSettings();
                }
            }
        }
        return this.jwtSettings;
    }
}
