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

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.data.Offset;
import org.java_websocket.client.WebSocketClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.rule.engine.api.NotificationManager;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
public class NotificationApiTest extends AbstractControllerTest {

    @Autowired
    private NotificationManager notificationManager;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSubscribingToUnreadNotificationsCount() {
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), notificationText1);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), notificationText2);

        getWsClient().subscribeForUnreadNotificationsCount();
        getWsClient().waitForReply(true);

        UnreadNotificationsCountUpdate update = getWsClient().getLastCountUpdate();
        assertThat(update.getTotalUnreadCount()).isEqualTo(2);
    }

    @Test
    public void testReceivingCountUpdates_multipleSessions() {
        getWsClient().subscribeForUnreadNotificationsCount();
        getAnotherWsClient().subscribeForUnreadNotificationsCount();
        getWsClient().waitForReply(true);
        getAnotherWsClient().waitForReply(true);
        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isZero();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Notification";
        submitNotificationRequest(notificationTarget.getId(), notificationText);
        getWsClient().waitForUpdate(true);
        getAnotherWsClient().waitForUpdate(true);

        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
    }

    @Test
    public void testSubscribingToUnreadNotifications_multipleSessions() throws Exception {
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), notificationText1);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), notificationText2);

        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply(true);
        getAnotherWsClient().waitForReply(true);

        checkFullNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText1, notificationText2);
        checkFullNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText1, notificationText2);
    }

    @Test
    public void testReceivingNotificationUpdates_multipleSessions() {
        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply(true);
        getAnotherWsClient().waitForReply(true);
        UnreadNotificationsUpdate notificationsUpdate = getWsClient().getLastDataUpdate();
        assertThat(notificationsUpdate.getTotalUnreadCount()).isZero();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), notificationText);
        getWsClient().waitForUpdate(true);
        getAnotherWsClient().waitForUpdate(true);

        checkPartialNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText, 1);
        checkPartialNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText, 1);
    }

    @Test
    public void testMarkingAsRead_multipleSessions() {
        getWsClient().subscribeForUnreadNotifications(10);
        getAnotherWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply(true);
        getAnotherWsClient().waitForReply(true);
        getAnotherWsClient().subscribeForUnreadNotificationsCount();
        getAnotherWsClient().waitForReply(true);

        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        String notificationText1 = "Notification 1";
        submitNotificationRequest(notificationTarget.getId(), notificationText1);
        getWsClient().waitForUpdate(true);
        getAnotherWsClient().waitForUpdate(true);
        Notification notification1 = getWsClient().getLastDataUpdate().getUpdate();

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        String notificationText2 = "Notification 2";
        submitNotificationRequest(notificationTarget.getId(), notificationText2);
        getWsClient().waitForUpdate(true);
        getAnotherWsClient().waitForUpdate(true);
        assertThat(getWsClient().getLastDataUpdate().getTotalUnreadCount()).isEqualTo(2);
        assertThat(getAnotherWsClient().getLastDataUpdate().getTotalUnreadCount()).isEqualTo(2);
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isEqualTo(2);

        getWsClient().registerWaitForUpdate();
        getAnotherWsClient().registerWaitForUpdate(2);
        getWsClient().markNotificationAsRead(notification1.getUuidId());
        getWsClient().waitForUpdate(true);
        getAnotherWsClient().waitForUpdate(true);

        checkFullNotificationsUpdate(getWsClient().getLastDataUpdate(), notificationText2);
        checkFullNotificationsUpdate(getAnotherWsClient().getLastDataUpdate(), notificationText2);
        assertThat(getAnotherWsClient().getLastCountUpdate().getTotalUnreadCount()).isOne();
    }

    @Test
    public void testDelayedNotificationRequest() throws Exception {
        getWsClient().subscribeForUnreadNotifications(5);
        getWsClient().waitForReply(true);

        getWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Was scheduled for 5 sec";
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), notificationText, 5);
        assertThat(notificationRequest.getStatus()).isEqualTo(NotificationRequestStatus.SCHEDULED);
        await().atLeast(4, TimeUnit.SECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .until(() -> getWsClient().getLastMsg() != null);

        Notification delayedNotification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(delayedNotification).extracting(Notification::getText).isEqualTo(notificationText);
        assertThat(delayedNotification.getCreatedTime() - notificationRequest.getCreatedTime())
                .isCloseTo(TimeUnit.SECONDS.toMillis(5), Offset.offset(500L));
        assertThat(findNotificationRequest(notificationRequest.getId()).getStatus()).isEqualTo(NotificationRequestStatus.PROCESSED);
    }

    @Test
    public void whenNotificationRequestIsDeleted_thenDeleteNotifications() throws Exception {
        getWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply(true);

        getWsClient().registerWaitForUpdate();
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), "Test");
        getWsClient().waitForUpdate(true);
        assertThat(getWsClient().getNotifications()).singleElement().extracting(Notification::getRequestId)
                .isEqualTo(notificationRequest.getId());
        assertThat(getWsClient().getUnreadCount()).isOne();

        getWsClient().registerWaitForUpdate();
        deleteNotificationRequest(notificationRequest.getId());
        getWsClient().waitForUpdate(true);

        assertThat(getWsClient().getNotifications()).isEmpty();
        assertThat(getWsClient().getUnreadCount()).isZero();
        assertThat(getMyNotifications(false, 10)).size().isZero();
    }

    @Test
    public void whenNotificationRequestIsUpdated_thenUpdateNotifications() throws Exception {
        getWsClient().subscribeForUnreadNotifications(10);
        getWsClient().waitForReply(true);

        NotificationTarget notificationTarget = createNotificationTarget(tenantAdminUserId);
        String notificationText = "Text";
        getWsClient().registerWaitForUpdate();
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), notificationText);
        getWsClient().waitForUpdate(true);
        Notification initialNotification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(getMyNotifications(false, 10)).singleElement().isEqualTo(initialNotification);
        assertThat(initialNotification.getInfo()).isNotNull().isEqualTo(notificationRequest.getInfo());

        getWsClient().registerWaitForUpdate();
        NotificationInfo newNotificationInfo = new NotificationInfo();
        newNotificationInfo.setDescription("New description");
        notificationRequest.setInfo(newNotificationInfo);
        notificationManager.updateNotificationRequest(tenantId, notificationRequest);
        getWsClient().waitForUpdate(true);
        Notification updatedNotification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(updatedNotification.getInfo()).isEqualTo(newNotificationInfo);
        assertThat(getMyNotifications(false, 10)).singleElement().isEqualTo(updatedNotification);
    }

    @Test
    public void testNotificationUpdatesForUsersInTarget() throws Exception {
        Map<User, NotificationApiWsClient> wsSessions = createUsersAndSetUpWsSessions(100);
        wsSessions.forEach((user, wsClient) -> {
            wsClient.subscribeForUnreadNotifications(10);
            wsClient.waitForReply(true);
            wsClient.subscribeForUnreadNotificationsCount();
            wsClient.waitForReply(true);
        });

        NotificationTarget notificationTarget = new NotificationTarget();
        UserListNotificationTargetConfig config = new UserListNotificationTargetConfig();
        config.setUsersIds(wsSessions.keySet().stream().map(User::getUuidId).collect(Collectors.toList()));
        notificationTarget.setName("100 users");
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setConfiguration(config);
        notificationTarget = saveNotificationTarget(notificationTarget);

        wsSessions.forEach((user, wsClient) -> wsClient.registerWaitForUpdate(2));
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), "Hello, ${email}");
        await().atMost(5, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> wsSessions.values().stream()
                        .allMatch(wsClient -> wsClient.getLastDataUpdate() != null
                                && wsClient.getLastCountUpdate() != null));

        wsSessions.forEach((user, wsClient) -> {
            assertThat(wsClient.getLastDataUpdate().getTotalUnreadCount()).isOne();
            assertThat(wsClient.getLastCountUpdate().getTotalUnreadCount()).isOne();

            Notification notification = wsClient.getLastDataUpdate().getUpdate();
            assertThat(notification.getRecipientId()).isEqualTo(user.getId());
            assertThat(notification.getRequestId()).isEqualTo(notificationRequest.getId());
            assertThat(notification.getText()).isEqualTo("Hello, " + user.getEmail());
        });
        wsSessions.values().forEach(WebSocketClient::close);
    }

    private Map<User, NotificationApiWsClient> createUsersAndSetUpWsSessions(int count) throws Exception {
        List<User> users = new LinkedList<>();
        for (int i = 1; i <= count; i++) {
            User user = new User();
            user.setTenantId(tenantId);
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setEmail("test-user-" + i + "@thingsboard.org");
            users.add(createUser(user, "12345678"));
        }
        Map<User, NotificationApiWsClient> wsSessions = new HashMap<>();
        for (User user : users) {
            login(user.getEmail(), "12345678");
            NotificationApiWsClient wsClient = (NotificationApiWsClient) buildAndConnectWebSocketClient();
            wsSessions.put(user, wsClient);
        }
        loginTenantAdmin();
        return wsSessions;
    }


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
        return saveNotificationTarget(notificationTarget);
    }

    private NotificationTarget saveNotificationTarget(NotificationTarget notificationTarget) {
        return doPost("/api/notification/target", notificationTarget, NotificationTarget.class);
    }

    private NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text) {
        return submitNotificationRequest(targetId, text, 0);
    }

    private NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(delayInSec);
        NotificationInfo notificationInfo = new NotificationInfo();
        notificationInfo.setDescription("The text: " + text);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targetId(targetId)
                .type("Test")
                .templateId(notificationTemplate.getId())
                .info(notificationInfo)
                .deliveryMethods(List.of(NotificationDeliveryMethod.WEBSOCKET))
                .additionalConfig(config)
                .build();
        return doPost("/api/notification/request", notificationRequest, NotificationRequest.class);
    }

    private NotificationRequest findNotificationRequest(NotificationRequestId id) throws Exception {
        return doGet("/api/notification/request/" + id, NotificationRequest.class);
    }

    private void deleteNotificationRequest(NotificationRequestId id) throws Exception {
        doDelete("/api/notification/request/" + id);
    }

    private List<Notification> getMyNotifications(boolean unreadOnly, int limit) throws Exception {
        return doGetTypedWithPageLink("/api/notifications?unreadOnly={unreadOnly}&", new TypeReference<PageData<Notification>>() {},
                new PageLink(limit, 0), unreadOnly).getData();
    }

    @Override
    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        NotificationApiWsClient wsClient = new NotificationApiWsClient(WS_URL + wsPort, token);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return wsClient;
    }

    @Override
    public NotificationApiWsClient getWsClient() {
        return (NotificationApiWsClient) super.getWsClient();
    }

    @Override
    public NotificationApiWsClient getAnotherWsClient() {
        return (NotificationApiWsClient) super.getAnotherWsClient();
    }

}
