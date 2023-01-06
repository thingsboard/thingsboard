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
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.DaoUtil;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractNotificationApiTest extends AbstractControllerTest {

    protected NotificationApiWsClient wsClient;
    protected NotificationApiWsClient otherWsClient;

    @MockBean
    protected SlackService slackService;

    @Autowired
    protected MailService mailService;

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.ADMIN;

    protected NotificationTarget createNotificationTarget(UserId... usersIds) {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setTenantId(tenantId);
        notificationTarget.setName("Users " + List.of(usersIds));
        UserListNotificationTargetConfig config = new UserListNotificationTargetConfig();
        config.setUsersIds(DaoUtil.toUUIDs(List.of(usersIds)));
        notificationTarget.setConfiguration(config);
        return saveNotificationTarget(notificationTarget);
    }

    protected NotificationTarget saveNotificationTarget(NotificationTarget notificationTarget) {
        return doPost("/api/notification/target", notificationTarget, NotificationTarget.class);
    }

    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(targetId, text, 0, deliveryMethods);
    }

    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(List.of(targetId), text, delayInSec, deliveryMethods);
    }

    protected NotificationRequest submitNotificationRequest(List<NotificationTargetId> targets, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        if (deliveryMethods.length == 0) {
            deliveryMethods = new NotificationDeliveryMethod[]{NotificationDeliveryMethod.PUSH};
        }
        NotificationTemplate notificationTemplate = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, text, deliveryMethods);
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(delayInSec);
        NotificationInfo notificationInfo = new NotificationInfo();
        notificationInfo.setDescription("The text: " + text);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targets(targets)
                .templateId(notificationTemplate.getId())
                .info(notificationInfo)
                .deliveryMethods(List.of(deliveryMethods))
                .additionalConfig(config)
                .build();
        return doPost("/api/notification/request", notificationRequest, NotificationRequest.class);
    }

    protected NotificationTemplate createNotificationTemplate(NotificationType notificationType, String subject,
                                                              String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName("Notification template for testing");
        notificationTemplate.setNotificationType(notificationType);
        notificationTemplate.setNotificationSubject(subject);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDefaultTextTemplate(text);
        config.setTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            if (deliveryMethod == NotificationDeliveryMethod.EMAIL) {
                EmailDeliveryMethodNotificationTemplate emailNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
                emailNotificationTemplate.setSubject("Hello from test");
                emailNotificationTemplate.setMethod(deliveryMethod);
                config.getTemplates().put(deliveryMethod, emailNotificationTemplate);
            } else {
                DeliveryMethodNotificationTemplate defaultTemplate = new DeliveryMethodNotificationTemplate();
                defaultTemplate.setMethod(deliveryMethod);
                config.getTemplates().put(deliveryMethod, defaultTemplate);
            }
        }
        notificationTemplate.setConfiguration(config);
        return doPost("/api/notification/template", notificationTemplate, NotificationTemplate.class);
    }

    protected Pair<User, NotificationApiWsClient> createUserAndConnectWsClient(Authority authority) throws Exception {
        User user = new User();
        user.setTenantId(tenantId);
        user.setAuthority(authority);
        user.setEmail(RandomStringUtils.randomAlphabetic(20) + "@thingsboard.com");
        user = createUserAndLogin(user, "12345678");
        NotificationApiWsClient wsClient = buildAndConnectWebSocketClient();
        return Pair.of(user, wsClient);
    }

    protected NotificationRequest findNotificationRequest(NotificationRequestId id) throws Exception {
        return doGet("/api/notification/request/" + id, NotificationRequest.class);
    }

    protected void deleteNotificationRequest(NotificationRequestId id) throws Exception {
        doDelete("/api/notification/request/" + id);
    }

    protected List<Notification> getMyNotifications(boolean unreadOnly, int limit) throws Exception {
        return doGetTypedWithPageLink("/api/notifications?unreadOnly={unreadOnly}&", new TypeReference<PageData<Notification>>() {},
                new PageLink(limit, 0), unreadOnly).getData();
    }

    @Override
    protected NotificationApiWsClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        NotificationApiWsClient wsClient = new NotificationApiWsClient(WS_URL + wsPort, token);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return wsClient;
    }

    protected void connectWsClient() throws Exception {
        loginCustomerUser();
        wsClient = (NotificationApiWsClient) super.getWsClient();
        loginTenantAdmin();
    }

    protected void connectOtherWsClient() throws Exception {
        loginCustomerUser();
        otherWsClient = (NotificationApiWsClient) super.getAnotherWsClient();
        loginTenantAdmin();
    }

}
