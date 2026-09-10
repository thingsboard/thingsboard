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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.notification.ApiKeyExpiryWarningNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;

import java.time.Duration;
import java.util.Comparator;
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

    @PostConstruct
    private void validateConfig() {
        if (tbClient.getAuthMode() == TbClient.AuthMode.API_KEY && StringUtils.isBlank(apiKeyDescription)) {
            log.warn("monitoring.rest.api_key_description is not set - API key expiry monitoring is disabled");
        }
    }

    @Scheduled(initialDelayString = "${monitoring.rest.api_key_expiry_check_initial_delay_ms:60000}",
            fixedDelayString = "${monitoring.rest.api_key_expiry_check_interval_ms:86400000}")
    void checkApiKeyExpiry() {
        if (tbClient.getAuthMode() != TbClient.AuthMode.API_KEY || StringUtils.isBlank(apiKeyDescription)) {
            return;
        }
        try {
            Optional<User> user = tbClient.getUser();
            if (user.isEmpty()) {
                log.warn("Failed to check API key expiry: could not resolve the current user");
                return;
            }
            resolveMonitoredApiKey(user.get().getId()).ifPresent(this::notifyIfExpiringOrExpired);
        } catch (HttpClientErrorException.Unauthorized e) {
            // the monitored key also authenticates this very call, so a 401 here - not
            // apiKeyInfo.isExpired() below - is the only way real-world expiry is ever observed:
            // once the key actually expires, the server rejects it before getUser()/getUserApiKeys() return
            notificationService.sendNotification(ApiKeyExpiryWarningNotification.expired(apiKeyDescription));
        } catch (Exception e) {
            log.warn("Failed to check API key expiry", e);
        }
    }

    // descriptions aren't unique - if rotation left an old key behind with the same description,
    // prefer enabled ones, and among several enabled matches, the one closest to expiring; a
    // never-expiring match is treated as infinitely far away so it's picked last
    private Optional<ApiKeyInfo> resolveMonitoredApiKey(UserId userId) {
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
            return Optional.empty();
        }
        if (enabledMatches.size() > 1) {
            log.warn("Found {} enabled API keys with description '{}' - descriptions should be unique; using the one closest to expiring for expiry checks", enabledMatches.size(), apiKeyDescription);
        }
        return enabledMatches.stream()
                .min(Comparator.comparingLong(key -> key.getExpirationTime() == 0 ? Long.MAX_VALUE : key.getExpirationTime()));
    }

    private void notifyIfExpiringOrExpired(ApiKeyInfo apiKeyInfo) {
        if (apiKeyInfo.isExpired()) {
            notificationService.sendNotification(ApiKeyExpiryWarningNotification.expired(apiKeyDescription));
            return;
        }
        if (apiKeyInfo.getExpirationTime() == 0) {
            return;
        }
        long millisLeft = apiKeyInfo.getExpirationTime() - System.currentTimeMillis();
        long daysLeft = Math.ceilDiv(millisLeft, Duration.ofDays(1).toMillis());
        if (daysLeft > warningDays) {
            return;
        }
        // ceilDiv rounds any remaining time up to a full day, so daysLeft == 1 covers anywhere
        // from 24h down to a few minutes left - call that out explicitly instead of the
        // misleadingly reassuring "expires in 1 day" for what may be an imminent expiry
        if (daysLeft <= 1) {
            notificationService.sendNotification(ApiKeyExpiryWarningNotification.expiringWithinADay(apiKeyDescription));
        } else {
            notificationService.sendNotification(ApiKeyExpiryWarningNotification.expiringIn(apiKeyDescription, daysLeft));
        }
    }

}
