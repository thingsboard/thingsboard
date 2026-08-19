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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
import org.thingsboard.monitoring.data.notification.Notification;

public class ApiKeyExpiryMonitoringServiceTest {

    private TbClient tbClient;
    private NotificationService notificationService;
    private ApiKeyExpiryMonitoringService service;
    private UserId userId;

    @BeforeEach
    public void setUp() {
        tbClient = mock(TbClient.class);
        notificationService = mock(NotificationService.class);
        service = new ApiKeyExpiryMonitoringService(tbClient, notificationService);
        ReflectionTestUtils.setField(service, "apiKeyDescription", "tb-monitoring key");
        ReflectionTestUtils.setField(service, "warningDays", 7);

        userId = new UserId(UUID.randomUUID());
        User user = mock(User.class);
        doReturn(userId).when(user).getId();
        doReturn(Optional.of(user)).when(tbClient).getUser();
        doReturn(TbClient.AuthMode.API_KEY).when(tbClient).getAuthMode();
    }

    private ApiKeyInfo apiKeyInfo(String description, long expirationTime) {
        return apiKeyInfo(description, expirationTime, true);
    }

    private ApiKeyInfo apiKeyInfo(String description, long expirationTime, boolean enabled) {
        ApiKeyInfo info = new ApiKeyInfo();
        info.setDescription(description);
        info.setExpirationTime(expirationTime);
        info.setEnabled(enabled);
        return info;
    }

    @Test
    public void loginMode_noRestCallsMade() {
        doReturn(TbClient.AuthMode.LOGIN).when(tbClient).getAuthMode();

        service.checkApiKeyExpiry();

        verify(tbClient, never()).getUser();
        verify(tbClient, never()).getUserApiKeys(any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void blankDescription_noRestCallsMade() {
        ReflectionTestUtils.setField(service, "apiKeyDescription", "");

        service.checkApiKeyExpiry();

        verify(tbClient, never()).getUser();
        verify(tbClient, never()).getUserApiKeys(any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void noMatchingKey_noNotificationNoException() {
        doReturn(new PageData<>(List.of(apiKeyInfo("some other key", 0)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void neverExpiringKey_noNotification_andSecondCallSkipsRestLookup() {
        doReturn(new PageData<>(List.of(apiKeyInfo("tb-monitoring key", 0)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();
        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
        verify(tbClient, times(1)).getUserApiKeys(any(), any());
    }

    @Test
    public void daysLeftAboveThreshold_noNotification() {
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        doReturn(new PageData<>(List.of(apiKeyInfo("tb-monitoring key", farFuture)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void daysLeftAtOrBelowThreshold_notifiesEveryCall_noDeduplication() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        doReturn(new PageData<>(List.of(apiKeyInfo("tb-monitoring key", soon)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();
        service.checkApiKeyExpiry();

        verify(notificationService, times(2)).sendNotification(any());
    }

    @Test
    public void restCallThrows_caughtNoPropagationNoNotification() {
        doThrow(new RuntimeException("network error")).when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void alreadyExpiredKey_sendsNotificationWithExpiredFlag() {
        long sixHoursAgo = System.currentTimeMillis() - Duration.ofHours(6).toMillis();
        doReturn(new PageData<>(List.of(apiKeyInfo("tb-monitoring key", sixHoursAgo)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).sendNotification(notificationCaptor.capture());
        String notificationText = notificationCaptor.getValue().getText();
        assertTrue(notificationText.contains("EXPIRED"), "Notification should contain 'EXPIRED' for an already-expired key");
        assertFalse(notificationText.contains("expires in 0 day"), "Notification should not contain 'expires in 0 day' for expired key");
    }

    @Test
    public void allMatchesDisabled_noNotificationNoException() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        doReturn(new PageData<>(List.of(apiKeyInfo("tb-monitoring key", soon, false)), 1, 1, false))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void disabledAndEnabledMatch_prefersEnabledOne() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        doReturn(new PageData<>(List.of(
                apiKeyInfo("tb-monitoring key", soon, false),
                apiKeyInfo("tb-monitoring key", farFuture, true)
        ), 1, 1, false)).when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    public void multipleEnabledMatches_picksOneAndStillFunctions() {
        // the far-future key is listed first - if the service picked the second (soon-expiring)
        // one instead, this would incorrectly send a notification
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        doReturn(new PageData<>(List.of(
                apiKeyInfo("tb-monitoring key", farFuture, true),
                apiKeyInfo("tb-monitoring key", soon, true)
        ), 1, 1, false)).when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

}
