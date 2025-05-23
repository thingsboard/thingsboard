/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.net.URISyntaxException;
import java.util.Arrays;
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

    @Autowired
    protected NotificationRuleService notificationRuleService;
    @Autowired
    protected NotificationTemplateService notificationTemplateService;
    @Autowired
    protected NotificationTargetService notificationTargetService;
    @Autowired
    protected NotificationRequestService notificationRequestService;
    @Autowired
    protected NotificationSettingsService notificationSettingsService;
    @Autowired
    protected SqlPartitioningRepository partitioningRepository;
    @Autowired
    protected DefaultNotifications defaultNotifications;

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.GENERAL;

    @After
    public void afterEach() {
        notificationRequestService.deleteNotificationRequestsByTenantId(TenantId.SYS_TENANT_ID);
        notificationRuleService.deleteNotificationRulesByTenantId(TenantId.SYS_TENANT_ID);
        notificationTemplateService.deleteNotificationTemplatesByTenantId(TenantId.SYS_TENANT_ID);
        notificationTargetService.deleteNotificationTargetsByTenantId(TenantId.SYS_TENANT_ID);
        partitioningRepository.cleanupPartitionsCache("notification", Long.MAX_VALUE, 0);
        notificationSettingsService.deleteNotificationSettings(TenantId.SYS_TENANT_ID);
    }

    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(targetId, text, 0, deliveryMethods);
    }

    protected NotificationRequest submitNotificationRequest(NotificationType type, NotificationTargetId targetId, String text, NotificationDeliveryMethod... deliveryMethods) {
        if (deliveryMethods.length == 0) {
            deliveryMethods = new NotificationDeliveryMethod[]{NotificationDeliveryMethod.WEB};
        }
        NotificationTemplate notificationTemplate = createNotificationTemplate(type, DEFAULT_NOTIFICATION_SUBJECT, text, deliveryMethods);
        return submitNotificationRequest(List.of(targetId), notificationTemplate.getId(), 0);
    }

    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(List.of(targetId), text, delayInSec, deliveryMethods);
    }

    protected NotificationRequest submitNotificationRequest(List<NotificationTargetId> targets, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        if (deliveryMethods.length == 0) {
            deliveryMethods = new NotificationDeliveryMethod[]{NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.MOBILE_APP};
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

    protected void saveNotificationSettings(NotificationSettings notificationSettings) throws Exception {
        doPost("/api/notification/settings", notificationSettings).andExpect(status().isOk());
    }

    protected void saveNotificationSettings(NotificationDeliveryMethodConfig... configs) throws Exception {
        NotificationSettings settings = new NotificationSettings();
        settings.setDeliveryMethodsConfigs(Arrays.stream(configs)
                .collect(Collectors.toMap(
                        NotificationDeliveryMethodConfig::getMethod, config -> config
                )));
        saveNotificationSettings(settings);
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

    protected NotificationRule createNotificationRule(NotificationRuleTriggerConfig triggerConfig, String subject, String text, NotificationTargetId... targets) {
        return createNotificationRule(triggerConfig, subject, text, List.of(targets), NotificationDeliveryMethod.WEB);
    }

    protected NotificationRule createNotificationRule(NotificationRuleTriggerConfig triggerConfig, String subject, String text, List<NotificationTargetId> targets, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate template = createNotificationTemplate(NotificationType.valueOf(triggerConfig.getTriggerType().toString()), subject, text, deliveryMethods);

        NotificationRule rule = new NotificationRule();
        rule.setName(triggerConfig.getTriggerType() + " " + targets);
        rule.setEnabled(true);
        rule.setTemplateId(template.getId());
        rule.setTriggerType(triggerConfig.getTriggerType());
        rule.setTriggerConfig(triggerConfig);

        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(triggerConfig.getTriggerType());
        recipientsConfig.setTargets(DaoUtil.toUUIDs(targets));
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
        NotificationApiWsClient wsClient = new NotificationApiWsClient(WS_URL + wsPort);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        wsClient.authenticate(token);
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
