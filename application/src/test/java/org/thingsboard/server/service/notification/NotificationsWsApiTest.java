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
package org.thingsboard.server.service.notification;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class NotificationsWsApiTest extends AbstractControllerTest {

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSubscribingToUnreadNotificationsCount() {
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText1);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText2);

        getWsClient().subscribeForUnreadNotificationsCount();
        getWsClient().waitForReply();

        UnreadNotificationsCountUpdate update = getWsClient().getLastCountUpdate();
        assertThat(update.getTotalUnreadCount()).isEqualTo(2);
    }

    @Test
    public void testReceivingCountUpdates_multipleSessions() {
        getWsClient().subscribeForUnreadNotificationsCount();
        getAnotherWsClient().subscribeForUnreadNotificationsCount();
        getWsClient().waitForReply();
        getAnotherWsClient().waitForReply();
        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isZero();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Notification";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText);
        getWsClient().waitForUpdate();
        getAnotherWsClient().waitForUpdate();

        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
    }

    @Test
    public void testSubscribingToUnreadNotifications_multipleSessions() throws Exception {
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText1);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText2);

        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply();
        getAnotherWsClient().waitForReply();

        checkFullNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText1, notificationText2);
        checkFullNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText1, notificationText2);
    }

    @Test
    public void testReceivingNotificationUpdates_multipleSessions() {
        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply();
        getAnotherWsClient().waitForReply();
        UnreadNotificationsUpdate notificationsUpdate = getWsClient().getLastDataUpdate();
        assertThat(notificationsUpdate.getTotalUnreadCount()).isZero();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText);
        getWsClient().waitForUpdate();
        getAnotherWsClient().waitForUpdate();

        checkPartialNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText, 1);
        checkPartialNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText, 1);
    }

    @Test
    public void testMarkingAsRead_multipleSessions() {
        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply();
        getAnotherWsClient().waitForReply();
        getAnotherWsClient().subscribeForUnreadNotificationsCount();
        getAnotherWsClient().waitForReply();

        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText1);
        getWsClient().waitForUpdate();
        getAnotherWsClient().waitForUpdate();
        Notification notification1 = getWsClient().getLastDataUpdate().getUpdate();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), "Just a test", notificationText2);
        getWsClient().waitForUpdate();
        getAnotherWsClient().waitForUpdate();
        assertThat(getWsClient().getLastDataUpdate().getTotalUnreadCount()).isEqualTo(2);
        assertThat(getAnotherWsClient().getLastDataUpdate().getTotalUnreadCount()).isEqualTo(2);
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isEqualTo(2);

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        getWsClient().markNotificationAsRead(notification1.getUuidId());
        getWsClient().waitForUpdate();
        getAnotherWsClient().waitForUpdate();

        checkFullNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText2);
        checkFullNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText2);
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
    }

    public void testReceivingUpdatesWhenSubscriptionAtAnotherInstance() {}


    private void checkFullNotificationsUpdate(UnreadNotificationsUpdate notificationsUpdate, String... expectedNotifications) {
        assertThat(notificationsUpdate.getNotifications()).extracting(Notification::getText).containsOnly(expectedNotifications);
        assertThat(notificationsUpdate.getTotalUnreadCount()).isEqualTo(expectedNotifications.length);
    }

    private void checkPartialNotificationsUpdate(UnreadNotificationsUpdate notificationsUpdate, String expectedNotification, int expectedUnreadCount) {
        assertThat(notificationsUpdate.getUpdate()).extracting(Notification::getText).isEqualTo(expectedNotification);
        assertThat(notificationsUpdate.getTotalUnreadCount()).isEqualTo(expectedUnreadCount);
    }

    private NotificationTarget createNotificationTarget(UserId userId) {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("User " + userId);
        SingleUserNotificationTargetConfig config = new SingleUserNotificationTargetConfig();
        config.setUserId(userId.getId());
        notificationTarget.setConfiguration(config);
        return doPost("/api/notification/target", notificationTarget, NotificationTarget.class);
    }

    private NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String notificationReason, String text) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targetId(targetId)
                .notificationReason(notificationReason)
                .textTemplate(text)
                .build();
        return doPost("/api/notification/request", notificationRequest, NotificationRequest.class);
    }

    @Override
    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        NotificationsWebSocketClient wsClient = new NotificationsWebSocketClient(WS_URL + wsPort, token);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return wsClient;
    }

    @Override
    public NotificationsWebSocketClient getWsClient() {
        return (NotificationsWebSocketClient) super.getWsClient();
    }

    @Override
    public NotificationsWebSocketClient getAnotherWsClient() {
        return (NotificationsWebSocketClient) super.getAnotherWsClient();
    }

}
