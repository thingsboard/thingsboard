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
package org.thingsboard.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "security.jwt")
@Data
@Slf4j
public class JwtSettings {
    public static final String ADMIN_SETTINGS_JWT_KEY = "jwt";
    static final String TOKEN_SIGNING_KEY_DEFAULT = "thingsboardDefaultSigningKey";
    /**
     * {@link JwtToken} will expire after this time.
     */
    private Integer tokenExpirationTime;

    /**
     * Token issuer.
     */
    private String tokenIssuer;

    /**
     * Key is used to sign {@link JwtToken}.
     * Base64 encoded
     */
    private String tokenSigningKey;

    /**
     * {@link JwtToken} can be refreshed during this timeframe.
     */
    private Integer refreshTokenExpTime;

    @JsonIgnore
    @Autowired
    private AdminSettingsService adminSettingsService;

    @PostConstruct
    public void init() {
        AdminSettings adminJwtSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (adminJwtSettings != null) {
            log.debug("Loading the JWT admin settings from database");
            JwtSettings jwtSettings = JacksonUtil.treeToValue(adminJwtSettings.getJsonValue(), JwtSettings.class);
            this.setRefreshTokenExpTime(jwtSettings.getRefreshTokenExpTime());
            this.setTokenExpirationTime(jwtSettings.getTokenExpirationTime());
            this.setTokenIssuer(jwtSettings.getTokenIssuer());
            this.setTokenSigningKey(jwtSettings.getTokenSigningKey());
        }

        if (hasDefaultTokenSigningKey()) {
            log.warn("JWT token signing key is default. This is a security issue. Please, consider to set unique value");
        }
    }

    public boolean hasDefaultTokenSigningKey() {
        return TOKEN_SIGNING_KEY_DEFAULT.equals(tokenSigningKey);
    }

}
