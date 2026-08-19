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
package org.thingsboard.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.notification.ApiKeyExpiryWarningNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyExpiryMonitoringService {

    private final TbClient tbClient;
    private final NotificationService notificationService;

    @Value("${monitoring.rest.api_key_description:}")
    private String apiKeyDescription;
    @Value("${monitoring.rest.api_key_expiry_warning_days:7}")
    private int warningDays;

    // expirationTime specifically can't be changed after creation, so this can never go stale
    private volatile boolean neverExpiresConfirmed = false;

    @Scheduled(initialDelay = 60_000, fixedDelay = 86_400_000)
    void checkApiKeyExpiry() {
        if (tbClient.getAuthMode() != TbClient.AuthMode.API_KEY) {
            return;
        }
        if (StringUtils.isBlank(apiKeyDescription)) {
            log.warn("monitoring.rest.api_key_description is not set - API key expiry monitoring is disabled");
            return;
        }
        if (neverExpiresConfirmed) {
            return;
        }

        try {
            Optional<User> user = tbClient.getUser();
            if (user.isEmpty()) {
                log.warn("Failed to check API key expiry: could not resolve the current user");
                return;
            }
            UserId userId = user.get().getId();
            // page size generously bounds this to any realistic number of API keys per user - full pagination isn't worth the complexity here
            PageData<ApiKeyInfo> page = tbClient.getUserApiKeys(userId, new PageLink(1000, 0, apiKeyDescription));
            List<ApiKeyInfo> matches = page.getData().stream()
                    .filter(key -> apiKeyDescription.equals(key.getDescription()))
                    .toList();
            List<ApiKeyInfo> enabledMatches = matches.stream().filter(ApiKeyInfo::isEnabled).toList();
            if (enabledMatches.isEmpty()) {
                if (matches.isEmpty()) {
                    log.warn("No API key found with description '{}' - cannot check expiry", apiKeyDescription);
                } else {
                    log.warn("Found {} API key(s) with description '{}' but all are disabled - cannot check expiry", matches.size(), apiKeyDescription);
                }
                return;
            }
            // descriptions aren't unique - if rotation left an old key behind with the same
            // description, prefer enabled ones, but the choice among several enabled matches is
            // still arbitrary - warn loudly so an operator notices instead of silently tracking
            // the wrong key
            if (enabledMatches.size() > 1) {
                log.warn("Found {} enabled API keys with description '{}' - descriptions should be unique; using an arbitrary one for expiry checks", enabledMatches.size(), apiKeyDescription);
            }

            ApiKeyInfo apiKeyInfo = enabledMatches.get(0);
            if (apiKeyInfo.getExpirationTime() == 0) {
                neverExpiresConfirmed = true;
                log.info("API key '{}' has no expiration - expiry monitoring disabled for the rest of this process's lifetime", apiKeyDescription);
                return;
            }

            long now = System.currentTimeMillis();
            boolean expired = apiKeyInfo.getExpirationTime() <= now;
            long daysLeft = Duration.ofMillis(apiKeyInfo.getExpirationTime() - now).toDays();
            if (expired || daysLeft <= warningDays) {
                notificationService.sendNotification(new ApiKeyExpiryWarningNotification(apiKeyDescription, daysLeft, expired));
            }
        } catch (Exception e) {
            log.warn("Failed to check API key expiry", e);
        }
    }

}
