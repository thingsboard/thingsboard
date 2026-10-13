// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.common.data.mail.MailOauth2Provider.OFFICE_365;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenExpCheckService {

    public static final int AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 90;

    private final TenantService tenantService;
    private final AdminSettingsService adminSettingsService;
    private final SecretConfigurationService secretConfigurationService;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${mail.oauth2.refreshTokenCheckingInterval})}",
            fixedDelayString = "${mail.oauth2.refreshTokenCheckingInterval}",
            timeUnit = TimeUnit.SECONDS)
    public void check() throws Exception {
        PageLink pageLink = new PageLink(1000);
        PageData<TenantId> tenantIds;
        do {
            tenantIds = tenantService.findTenantsIds(pageLink);
            for (TenantId tenantId : tenantIds.getData()) {
                try {
                    AdminSettings tenantMailSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, "mail");
                    refreshTokenIfExpires(tenantId, tenantMailSettings, adminSettingsService::saveAdminSettings);
                } catch (Exception e) {
                    log.error("[{}] Error occurred while checking token", tenantId, e);
                }
            }
            pageLink = pageLink.nextPageLink();
        } while (tenantIds.hasNext());

        AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        refreshTokenIfExpires(TenantId.SYS_TENANT_ID, systemMailSettings, adminSettingsService::saveAdminSettings);
    }

    private void refreshTokenIfExpires(TenantId tenantId, AdminSettings adminSettings, BiConsumer<TenantId, AdminSettings> saveFunction) throws Exception {
        if (adminSettings != null) {
            JsonNode jsonValue = adminSettings.getJsonValue().deepCopy();
            secretConfigurationService.replaceSecretUsages(tenantId, jsonValue);
            if (jsonValue != null && jsonValue.has("useSystemMailSettings") && !jsonValue.get("useSystemMailSettings").asBoolean() &&
                    jsonValue.has("enableOauth2") && jsonValue.get("enableOauth2").asBoolean() && OFFICE_365.name().equals(jsonValue.get("providerId").asText()) &&
                    jsonValue.has("refreshToken") && jsonValue.has("refreshTokenExpires")) {
                long expiresIn = jsonValue.get("refreshTokenExpires").longValue();
                long tokenLifeDuration = expiresIn - System.currentTimeMillis();
                if (tokenLifeDuration < 0) {
                    ((ObjectNode) adminSettings.getJsonValue()).put("tokenGenerated", false);
                    ((ObjectNode) adminSettings.getJsonValue()).remove("refreshToken");
                    ((ObjectNode) adminSettings.getJsonValue()).remove("refreshTokenExpires");

                    saveFunction.accept(tenantId, adminSettings);
                } else if (tokenLifeDuration < 604800000L) { //less than 7 days
                    log.info("Trying to refresh refresh token.");

                    String clientId = jsonValue.get("clientId").asText();
                    String clientSecret = jsonValue.get("clientSecret").asText();
                    String refreshToken = jsonValue.get("refreshToken").asText();
                    String tokenUri = jsonValue.get("tokenUri").asText();

                    TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new GsonFactory(),
                            new GenericUrl(tokenUri), refreshToken)
                            .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                            .execute();
                    ((ObjectNode) adminSettings.getJsonValue()).put("refreshToken", tokenResponse.getRefreshToken());
                    ((ObjectNode) adminSettings.getJsonValue()).put("refreshTokenExpires", Instant.now().plus(Duration.ofDays(AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)).toEpochMilli());
                    saveFunction.accept(tenantId, adminSettings);
                }
            }
        }
    }

}
