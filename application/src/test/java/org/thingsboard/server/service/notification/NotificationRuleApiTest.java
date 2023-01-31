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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationRuleApiTest extends AbstractNotificationApiTest {

    @SpyBean
    private AlarmSubscriptionService alarmSubscriptionService;
    @Autowired
    private NotificationRequestService notificationRequestService;

    @SpyBean
    private AlarmService alarmService;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testNotificationRuleProcessing_entityActionTrigger() throws Exception {
        String notificationSubject = "${actionType}: ${entityType} [${entityId}]";
        String notificationText = "User: ${originatorUserName}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.GENERAL, notificationSubject, notificationText, NotificationDeliveryMethod.PUSH);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Push-notification when any device is created, updated or deleted");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);

        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        triggerConfig.setEntityType(EntityType.DEVICE);
        triggerConfig.setCreated(true);
        triggerConfig.setUpdated(true);
        triggerConfig.setDeleted(true);

        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);
        recipientsConfig.setTargets(List.of(createNotificationTarget(tenantAdminUserId).getUuidId()));

        notificationRule.setTriggerConfig(triggerConfig);
        notificationRule.setRecipientsConfig(recipientsConfig);
        notificationRule = saveNotificationRule(notificationRule);

        getWsClient().subscribeForUnreadNotifications(10).waitForReply(true);


        getWsClient().registerWaitForUpdate();
        Device device = createDevice("DEVICE!!!", "default", "12345");
        getWsClient().waitForUpdate(true);

        Notification notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("ADDED: DEVICE [" + device.getId() + "]");
        assertThat(notification.getText()).isEqualTo("User: " + TENANT_ADMIN_EMAIL);


        getWsClient().registerWaitForUpdate();
        device.setName("Updated name");
        device = doPost("/api/device", device, Device.class);
        getWsClient().waitForUpdate(true);

        notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("UPDATED: DEVICE [" + device.getId() + "]");


        getWsClient().registerWaitForUpdate();
        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());
        getWsClient().waitForUpdate(true);

        notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("DELETED: DEVICE [" + device.getId() + "]");
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger() throws Exception {
        String notificationSubject = "Alarm type: ${alarmType}, status: ${alarmStatus}, " +
                "severity: ${alarmSeverity}, deviceId: ${alarmOriginatorId}";
        String notificationText = "Status: ${alarmStatus}, severity: ${alarmSeverity}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.PUSH);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Push-notification on any alarm");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ALARM);

        AlarmNotificationRuleTriggerConfig triggerConfig = new AlarmNotificationRuleTriggerConfig();
        triggerConfig.setAlarmTypes(null);
        triggerConfig.setAlarmSeverities(null);
        notificationRule.setTriggerConfig(triggerConfig);

        EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ALARM);
        Map<Integer, List<UUID>> escalationTable = new HashMap<>();
        recipientsConfig.setEscalationTable(escalationTable);
        Map<Integer, NotificationApiWsClient> clients = new HashMap<>();
        for (int delay = 0; delay <= 5; delay++) {
            Pair<User, NotificationApiWsClient> userAndClient = createUserAndConnectWsClient(Authority.TENANT_ADMIN);
            NotificationTarget notificationTarget = createNotificationTarget(userAndClient.getFirst().getId());
            escalationTable.put(delay, List.of(notificationTarget.getUuidId()));
            clients.put(delay, userAndClient.getSecond());
        }
        notificationRule.setRecipientsConfig(recipientsConfig);
        notificationRule = saveNotificationRule(notificationRule);


        String alarmType = "myBoolIsTrue";
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(notificationRule.getId(), alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "1234");

        clients.values().forEach(wsClient -> {
            wsClient.subscribeForUnreadNotifications(10).waitForReply(true);
            wsClient.registerWaitForUpdate();
        });

        JsonNode attr = JacksonUtil.newObjectNode()
                .set("bool", BooleanNode.TRUE);
        doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attr);

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get() != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get();

        long ts = System.currentTimeMillis();
        await().atMost(7, TimeUnit.SECONDS)
                .until(() -> clients.values().stream().allMatch(client -> client.getLastDataUpdate() != null));
        clients.forEach((expectedDelay, wsClient) -> {
            Notification notification = wsClient.getLastDataUpdate().getUpdate();
            double actualDelay = (double) (notification.getCreatedTime() - ts) / 1000;
            assertThat(actualDelay).isCloseTo(expectedDelay, offset(0.5));

            AlarmStatus expectedStatus = AlarmStatus.ACTIVE_UNACK;
            AlarmSeverity expectedSeverity = AlarmSeverity.CRITICAL;

            assertThat(notification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + expectedStatus + ", " +
                    "severity: " + expectedSeverity + ", deviceId: " + device.getId());
            assertThat(notification.getText()).isEqualTo("Status: " + expectedStatus + ", severity: " + expectedSeverity);

            assertThat(notification.getType()).isEqualTo(NotificationType.ALARM);
            assertThat(notification.getInfo()).isInstanceOf(AlarmNotificationInfo.class);
            AlarmNotificationInfo info = (AlarmNotificationInfo) notification.getInfo();
            assertThat(info.getAlarmId()).isEqualTo(alarm.getUuidId());
            assertThat(info.getAlarmType()).isEqualTo(alarmType);
            assertThat(info.getAlarmSeverity()).isEqualTo(expectedSeverity);
            assertThat(info.getAlarmStatus()).isEqualTo(expectedStatus);
        });

        clients.values().forEach(wsClient -> wsClient.registerWaitForUpdate());
        alarmSubscriptionService.ackAlarm(tenantId, alarm.getId(), System.currentTimeMillis());
        AlarmStatus expectedStatus = AlarmStatus.ACTIVE_ACK;
        AlarmSeverity expectedSeverity = AlarmSeverity.CRITICAL;
        clients.values().forEach(wsClient -> {
            wsClient.waitForUpdate(true);
            Notification updatedNotification = wsClient.getLastDataUpdate().getNotifications().stream().findFirst().get();
            assertThat(updatedNotification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + expectedStatus + ", " +
                    "severity: " + expectedSeverity + ", deviceId: " + device.getId());
            assertThat(updatedNotification.getText()).isEqualTo("Status: " + expectedStatus + ", severity: " + expectedSeverity);

            wsClient.close();
        });

        // TODO: test severity changes
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger_clearRule() throws Exception {
        String notificationSubject = "${alarmSeverity} alarm '${alarmType}' is ${alarmStatus}";
        String notificationText = "${alarmId}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.PUSH);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Push-notification on any alarm");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ALARM);

        String alarmType = "myBoolIsTrue";
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(notificationRule.getId(), alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "1234");

        AlarmNotificationRuleTriggerConfig triggerConfig = new AlarmNotificationRuleTriggerConfig();
        triggerConfig.setAlarmTypes(Set.of(alarmType));
        triggerConfig.setAlarmSeverities(null);

        AlarmNotificationRuleTriggerConfig.ClearRule clearRule = new AlarmNotificationRuleTriggerConfig.ClearRule();
        clearRule.setAlarmStatus(AlarmStatus.CLEARED_UNACK);
        triggerConfig.setClearRule(clearRule);
        notificationRule.setTriggerConfig(triggerConfig);

        EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ALARM);
        Map<Integer, List<UUID>> escalationTable = new HashMap<>();
        recipientsConfig.setEscalationTable(escalationTable);

        escalationTable.put(0, List.of(createNotificationTarget(tenantAdminUserId).getUuidId()));
        escalationTable.put(1000, List.of(createNotificationTarget(customerUserId).getUuidId()));

        notificationRule.setRecipientsConfig(recipientsConfig);
        notificationRule = saveNotificationRule(notificationRule);

        getWsClient().subscribeForUnreadNotifications(10).waitForReply(true);
        getWsClient().registerWaitForUpdate();
        JsonNode attr = JacksonUtil.newObjectNode()
                .set("bool", BooleanNode.TRUE);
        doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attr);

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get() != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get();
        getWsClient().waitForUpdate(true);

        Notification notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("CRITICAL alarm '" + alarmType + "' is ACTIVE_UNACK");
        assertThat(notification.getInfo()).asInstanceOf(type(AlarmNotificationInfo.class))
                .extracting(AlarmNotificationInfo::getAlarmId).isEqualTo(alarm.getUuidId());

        await().atMost(2, TimeUnit.SECONDS).until(() -> findNotificationRequests(EntityType.ALARM).getTotalElements() == escalationTable.size());
        NotificationRequestInfo scheduledNotificationRequest = findNotificationRequests(EntityType.ALARM).getData().stream()
                .filter(NotificationRequest::isScheduled)
                .findFirst().orElse(null);
        assertThat(scheduledNotificationRequest).extracting(NotificationRequest::getInfo).isEqualTo(notification.getInfo());

        getWsClient().registerWaitForUpdate();
        alarmSubscriptionService.clearAlarm(tenantId, alarm.getId(), null, System.currentTimeMillis());
        getWsClient().waitForUpdate(true);
        notification = getWsClient().getLastDataUpdate().getNotifications().iterator().next();
        assertThat(notification.getSubject()).isEqualTo("CRITICAL alarm '" + alarmType + "' is CLEARED_UNACK");

        assertThat(findNotificationRequests(EntityType.ALARM).getData()).filteredOn(NotificationRequest::isScheduled).isEmpty();
    }

    @Test
    public void testNotificationRuleInfo() throws Exception {
        NotificationDeliveryMethod[] deliveryMethods = {NotificationDeliveryMethod.PUSH, NotificationDeliveryMethod.EMAIL};
        NotificationTemplate template = createNotificationTemplate(NotificationType.ENTITY_ACTION, "Subject", "Text", deliveryMethods);

        NotificationRule rule = new NotificationRule();
        rule.setName("Test");
        rule.setTemplateId(template.getId());

        rule.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);
        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        rule.setTriggerConfig(triggerConfig);

        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);
        recipientsConfig.setTargets(List.of(createNotificationTarget(tenantAdminUserId).getUuidId()));
        rule.setRecipientsConfig(recipientsConfig);
        rule = saveNotificationRule(rule);

        NotificationRuleInfo ruleInfo = findNotificationRules().getData().get(0);
        assertThat(ruleInfo.getId()).isEqualTo(ruleInfo.getId());
        assertThat(ruleInfo.getTemplateName()).isEqualTo(template.getName());
        assertThat(ruleInfo.getDeliveryMethods()).containsOnly(deliveryMethods);
    }

    private DeviceProfile createDeviceProfileWithAlarmRules(NotificationRuleId notificationRuleId, String alarmType) {
        DeviceProfile deviceProfile = createDeviceProfile("For notification rule test");
        deviceProfile.setTenantId(tenantId);

        List<DeviceProfileAlarm> alarms = new ArrayList<>();
        DeviceProfileAlarm alarm = new DeviceProfileAlarm();
        alarm.setAlarmType(alarmType);
        alarm.setId(alarmType);
        alarm.setNotificationRuleId(notificationRuleId);
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Details");
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        List<AlarmConditionFilter> condition = new ArrayList<>();

        AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "bool"));
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        predicate.setValue(new FilterPredicateValue<>(true));

        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.BOOLEAN);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        alarm.setCreateRules(createRules);
        alarms.add(alarm);

        deviceProfile.getProfileData().setAlarms(alarms);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        return deviceProfile;
    }

    private NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        return doPost("/api/notification/rule", notificationRule, NotificationRule.class);
    }

    private PageData<NotificationRuleInfo> findNotificationRules() throws Exception {
        PageLink pageLink = new PageLink(10);
        return doGetTypedWithPageLink("/api/notification/rules?", new TypeReference<PageData<NotificationRuleInfo>>() {}, pageLink);
    }

    private PageData<NotificationRequestInfo> findNotificationRequests(EntityType originatorType) {
        return notificationRequestService.findNotificationRequestsInfosByTenantIdAndOriginatorType(tenantId, originatorType, new PageLink(100));
    }

}
