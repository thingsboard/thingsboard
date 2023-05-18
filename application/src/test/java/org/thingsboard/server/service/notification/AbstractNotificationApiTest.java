/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.DaoUtil;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractNotificationApiTest extends AbstractControllerTest {

    protected NotificationApiWsClient wsClient;
    protected NotificationApiWsClient otherWsClient;

    @MockBean
    protected SlackService slackService;

    @Autowired
    protected MailService mailService;

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.GENERAL;

    protected NotificationTarget createNotificationTarget(UserId... usersIds) {
        UserListFilter filter = new UserListFilter();
        filter.setUsersIds(DaoUtil.toUUIDs(List.of(usersIds)));
        return createNotificationTarget(filter);
    }

    protected NotificationTarget createNotificationTarget(UsersFilter usersFilter) {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName(usersFilter.toString());
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(usersFilter);
        notificationTarget.setConfiguration(targetConfig);
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
            deliveryMethods = new NotificationDeliveryMethod[]{NotificationDeliveryMethod.WEB};
        }
        NotificationTemplate notificationTemplate = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, text, deliveryMethods);
        return submitNotificationRequest(targets, notificationTemplate.getId(), delayInSec);
    }

    protected NotificationRequest submitNotificationRequest(List<NotificationTargetId> targets, NotificationTemplateId notificationTemplateId, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(delayInSec);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .targets(targets.stream().map(UUIDBased::getId).collect(Collectors.toList()))
                .templateId(notificationTemplateId)
                .additionalConfig(config)
                .build();
        return doPost("/api/notification/request", notificationRequest, NotificationRequest.class);
    }

    protected NotificationRequestStats getStats(NotificationRequestId notificationRequestId) throws Exception {
        return findNotificationRequest(notificationRequestId).getStats();
    }

    protected NotificationTemplate createNotificationTemplate(NotificationType notificationType, String subject,
                                                              String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = setUpNotificationTemplate(notificationType, subject, text, deliveryMethods);
        return saveNotificationTemplate(notificationTemplate);
    }

    protected NotificationTemplate setUpNotificationTemplate(NotificationType notificationType, String subject,
                                                             String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName("Notification template: " + text);
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDeliveryMethodsTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            DeliveryMethodNotificationTemplate deliveryMethodNotificationTemplate;
            switch (deliveryMethod) {
                case WEB: {
                    WebDeliveryMethodNotificationTemplate template = new WebDeliveryMethodNotificationTemplate();
                    template.setSubject(subject);
                    deliveryMethodNotificationTemplate = template;
                    break;
                }
                case EMAIL: {
                    EmailDeliveryMethodNotificationTemplate template = new EmailDeliveryMethodNotificationTemplate();
                    template.setSubject(subject);
                    deliveryMethodNotificationTemplate = template;
                    break;
                }
                case SMS: {
                    deliveryMethodNotificationTemplate = new SmsDeliveryMethodNotificationTemplate();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported delivery method " + deliveryMethod);
            }
            deliveryMethodNotificationTemplate.setEnabled(true);
            deliveryMethodNotificationTemplate.setBody(text);
            config.getDeliveryMethodsTemplates().put(deliveryMethod, deliveryMethodNotificationTemplate);
        }
        notificationTemplate.setConfiguration(config);
        return notificationTemplate;
    }

    protected NotificationTemplate saveNotificationTemplate(NotificationTemplate notificationTemplate) {
        return doPost("/api/notification/template", notificationTemplate, NotificationTemplate.class);
    }

    protected void saveNotificationSettings(NotificationSettings notificationSettings) throws Exception {
        doPost("/api/notification/settings", notificationSettings).andExpect(status().isOk());
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

    protected NotificationRequestInfo findNotificationRequest(NotificationRequestId id) throws Exception {
        return doGet("/api/notification/request/" + id, NotificationRequestInfo.class);
    }

    protected PageData<NotificationRequestInfo> findNotificationRequests() throws Exception {
        PageLink pageLink = new PageLink(10);
        return doGetTypedWithPageLink("/api/notification/requests?", new TypeReference<PageData<NotificationRequestInfo>>() {}, pageLink);
    }

    protected void deleteNotificationRequest(NotificationRequestId id) throws Exception {
        doDelete("/api/notification/request/" + id);
    }

    protected List<Notification> getMyNotifications(boolean unreadOnly, int limit) throws Exception {
        return doGetTypedWithPageLink("/api/notifications?unreadOnly={unreadOnly}&", new TypeReference<PageData<Notification>>() {},
                new PageLink(limit, 0), unreadOnly).getData();
    }

    protected NotificationRule createNotificationRule(NotificationRuleTriggerConfig triggerConfig, String subject, String text, NotificationTargetId... targets) {
        NotificationTemplate template = createNotificationTemplate(NotificationType.valueOf(triggerConfig.getTriggerType().toString()), subject, text, NotificationDeliveryMethod.WEB);

        NotificationRule rule = new NotificationRule();
        rule.setName(triggerConfig.getTriggerType() + " " + Arrays.toString(targets));
        rule.setTemplateId(template.getId());
        rule.setTriggerType(triggerConfig.getTriggerType());
        rule.setTriggerConfig(triggerConfig);

        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(triggerConfig.getTriggerType());
        recipientsConfig.setTargets(DaoUtil.toUUIDs(List.of(targets)));
        rule.setRecipientsConfig(recipientsConfig);

        return saveNotificationRule(rule);
    }

    protected NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        return doPost("/api/notification/rule", notificationRule, NotificationRule.class);
    }

    protected PageData<NotificationRuleInfo> findNotificationRules() throws Exception {
        PageLink pageLink = new PageLink(10);
        return doGetTypedWithPageLink("/api/notification/rules?", new TypeReference<PageData<NotificationRuleInfo>>() {}, pageLink);
    }

    @Override
    protected NotificationApiWsClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        NotificationApiWsClient wsClient = new NotificationApiWsClient(WS_URL + wsPort, token);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return wsClient;
    }

    @Override
    public NotificationApiWsClient getWsClient() {
        return (NotificationApiWsClient) super.getWsClient();
    }

}
