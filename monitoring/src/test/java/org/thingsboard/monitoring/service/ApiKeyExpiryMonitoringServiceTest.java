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
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.notification.Notification;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApiKeyExpiryMonitoringServiceTest {

    private TbClient tbClient;
    private NotificationService notificationService;
    private ApiKeyExpiryMonitoringService service;
    private UserId userId;

    @BeforeEach
    void setUp() {
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

    private void givenApiKeys(ApiKeyInfo... keys) {
        doReturn(new PageData<>(List.of(keys), 1, 1, false)).when(tbClient).getUserApiKeys(eq(userId), any());
    }

    private String sentNotificationText() {
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).sendNotification(notificationCaptor.capture());
        return notificationCaptor.getValue().getText();
    }

    @Test
    void loginMode_noRestCallsMade() {
        doReturn(TbClient.AuthMode.LOGIN).when(tbClient).getAuthMode();

        service.checkApiKeyExpiry();

        verify(tbClient, never()).getUser();
        verify(tbClient, never()).getUserApiKeys(any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void blankDescription_noRestCallsMade() {
        ReflectionTestUtils.setField(service, "apiKeyDescription", "");

        service.checkApiKeyExpiry();

        verify(tbClient, never()).getUser();
        verify(tbClient, never()).getUserApiKeys(any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void userLookupReturnsEmpty_noNotificationNoException() {
        doReturn(Optional.empty()).when(tbClient).getUser();

        service.checkApiKeyExpiry();

        verify(tbClient, never()).getUserApiKeys(any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void noMatchingKey_noNotificationNoException() {
        givenApiKeys(apiKeyInfo("some other key", 0));

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void neverExpiringKey_noNotification_reCheckedEveryCall() {
        givenApiKeys(apiKeyInfo("tb-monitoring key", 0));

        service.checkApiKeyExpiry();
        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
        verify(tbClient, times(2)).getUserApiKeys(any(), any());
    }

    @Test
    void daysLeftAboveThreshold_noNotification() {
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", farFuture));

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void daysLeftExactlyAtThreshold_notifies() {
        // just under warningDays*1day so real-clock jitter between setup and the service's own
        // "now" can only round this UP to warningDays via ceilDiv, never down past it
        long expirationTime = System.currentTimeMillis() + Duration.ofDays(7).toMillis() - Duration.ofSeconds(5).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", expirationTime));

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText()).contains("expires in 7 days");
    }

    @Test
    void daysLeftJustAboveThreshold_noNotification() {
        long expirationTime = System.currentTimeMillis() + Duration.ofDays(7).toMillis() + Duration.ofMinutes(1).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", expirationTime));

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void lessThanADayLeft_sendsUrgentWithinADayNotification_notMisleading1DayMessage() {
        long fiveMinutesLeft = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", fiveMinutesLeft));

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText())
                .contains("expires within a day")
                .doesNotContain("expires in 1 day");
    }

    @Test
    void daysLeftAtOrBelowThreshold_notifiesEveryCall_noDeduplication() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", soon));

        service.checkApiKeyExpiry();
        service.checkApiKeyExpiry();

        verify(notificationService, times(2)).sendNotification(any());
    }

    @Test
    void restCallThrows_caughtNoPropagationNoNotification() {
        doThrow(new RuntimeException("network error")).when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void alreadyExpiredKey_sendsNotificationWithExpiredFlag() {
        long sixHoursAgo = System.currentTimeMillis() - Duration.ofHours(6).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", sixHoursAgo));

        service.checkApiKeyExpiry();

        String notificationText = sentNotificationText();
        assertThat(notificationText).contains("EXPIRED");
        assertThat(notificationText).doesNotContain("expires in 0 day");
    }

    @Test
    void unauthorizedFromGetUser_sendsExpiredNotification() {
        // the only way real-world expiry is ever observed: the server rejects the very key that
        // authenticates this call, before apiKeyInfo.isExpired() is ever reached
        doThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null))
                .when(tbClient).getUser();

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText()).contains("EXPIRED");
    }

    @Test
    void unauthorizedFromGetUserApiKeys_sendsExpiredNotification() {
        doThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null))
                .when(tbClient).getUserApiKeys(eq(userId), any());

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText()).contains("EXPIRED");
    }

    @Test
    void allMatchesDisabled_noNotificationNoException() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        givenApiKeys(apiKeyInfo("tb-monitoring key", soon, false));

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void disabledAndEnabledMatch_prefersEnabledOne() {
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        givenApiKeys(
                apiKeyInfo("tb-monitoring key", soon, false),
                apiKeyInfo("tb-monitoring key", farFuture, true)
        );

        service.checkApiKeyExpiry();

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void multipleEnabledMatches_picksTheOneClosestToExpiringRegardlessOfListOrder() {
        // far-future key listed first - the service must still pick the soon-expiring one
        // (deterministically the closest to expiring), not just the first in the list
        long farFuture = System.currentTimeMillis() + Duration.ofDays(30).toMillis();
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        givenApiKeys(
                apiKeyInfo("tb-monitoring key", farFuture, true),
                apiKeyInfo("tb-monitoring key", soon, true)
        );

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText())
                .as("Should warn about the soon-expiring key, not the far-future one")
                .contains("expires in 3 days");
    }

    @Test
    void neverExpiringMatchAmongDuplicates_treatedAsFarthestAway() {
        // never-expiring key listed first - must not be picked over a soon-expiring duplicate
        long soon = System.currentTimeMillis() + Duration.ofDays(3).toMillis();
        givenApiKeys(
                apiKeyInfo("tb-monitoring key", 0, true),
                apiKeyInfo("tb-monitoring key", soon, true)
        );

        service.checkApiKeyExpiry();

        assertThat(sentNotificationText())
                .as("Should warn about the soon-expiring key, not the never-expiring one")
                .contains("expires in 3 days");
    }

}
