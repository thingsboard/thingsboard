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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRuleCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
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
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.notification.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.notification.NotificationRuleProcessor;
import org.thingsboard.server.service.apiusage.limits.LimitedApi;
import org.thingsboard.server.service.apiusage.limits.RateLimitService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testNotificationRuleProcessing_entityActionTrigger() throws Exception {
        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        triggerConfig.setEntityTypes(Set.of(EntityType.DEVICE));
        triggerConfig.setCreated(true);
        triggerConfig.setUpdated(true);
        triggerConfig.setDeleted(true);
        createNotificationRule(triggerConfig, "${actionType}: ${entityType} [${entityId}]",
                "User: ${userEmail}", createNotificationTarget(tenantAdminUserId).getId());

        Device device = checkNotificationAfter(() -> {
            return createDevice("DEVICE!!!", "default", "12345");
        }, (notification, newDevice) -> {
            assertThat(notification.getSubject()).isEqualTo("added: Device [" + newDevice.getId() + "]");
            assertThat(notification.getText()).isEqualTo("User: " + TENANT_ADMIN_EMAIL);
        });

        checkNotificationAfter(() -> {
            device.setName("Updated name");
            doPost("/api/device", device, Device.class);
        }, notification -> {
            assertThat(notification.getSubject()).isEqualTo("updated: Device [" + device.getId() + "]");
        });

        checkNotificationAfter(() -> {
            doDelete("/api/device/" + device.getId()).andExpect(status().isOk());
        }, notification -> {
            assertThat(notification.getSubject()).isEqualTo("deleted: Device [" + device.getId() + "]");
        });
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger() throws Exception {
        String notificationSubject = "Alarm type: ${alarmType}, status: ${alarmStatus}, " +
                "severity: ${alarmSeverity}, deviceId: ${alarmOriginatorId}";
        String notificationText = "Status: ${alarmStatus}, severity: ${alarmSeverity}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.WEB);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Web notification on any alarm");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ALARM);

        AlarmNotificationRuleTriggerConfig triggerConfig = new AlarmNotificationRuleTriggerConfig();
        triggerConfig.setAlarmTypes(null);
        triggerConfig.setAlarmSeverities(null);
        triggerConfig.setNotifyOn(Set.of(AlarmAction.CREATED, AlarmAction.SEVERITY_CHANGED, AlarmAction.ACKNOWLEDGED, AlarmAction.CLEARED));
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
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "1234");

        clients.values().forEach(wsClient -> {
            wsClient.subscribeForUnreadNotifications(10).waitForReply(true);
            wsClient.registerWaitForUpdate();
        });

        JsonNode attr = JacksonUtil.newObjectNode()
                .set("bool", BooleanNode.TRUE);
        doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attr);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get() != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get();

        long ts = System.currentTimeMillis();
        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> clients.values().stream().allMatch(client -> client.getLastDataUpdate() != null));
        clients.forEach((expectedDelay, wsClient) -> {
            Notification notification = wsClient.getLastDataUpdate().getUpdate();
            double actualDelay = (double) (notification.getCreatedTime() - ts) / 1000;
            assertThat(actualDelay).isCloseTo(expectedDelay, offset(2.0));

            assertThat(notification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + AlarmStatus.ACTIVE_UNACK + ", " +
                    "severity: " + AlarmSeverity.CRITICAL.toString().toLowerCase() + ", deviceId: " + device.getId());
            assertThat(notification.getText()).isEqualTo("Status: " + AlarmStatus.ACTIVE_UNACK + ", severity: " + AlarmSeverity.CRITICAL.toString().toLowerCase());

            assertThat(notification.getType()).isEqualTo(NotificationType.ALARM);
            assertThat(notification.getInfo()).isInstanceOf(AlarmNotificationInfo.class);
            AlarmNotificationInfo info = (AlarmNotificationInfo) notification.getInfo();
            assertThat(info.getAlarmId()).isEqualTo(alarm.getUuidId());
            assertThat(info.getAlarmType()).isEqualTo(alarmType);
            assertThat(info.getAlarmSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(info.getAlarmStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        clients.values().forEach(wsClient -> wsClient.registerWaitForUpdate());
        alarmSubscriptionService.acknowledgeAlarm(tenantId, alarm.getId(), System.currentTimeMillis());
        AlarmStatus expectedStatus = AlarmStatus.ACTIVE_ACK;
        AlarmSeverity expectedSeverity = AlarmSeverity.CRITICAL;
        clients.values().forEach(wsClient -> {
            wsClient.waitForUpdate(true);
            Notification updatedNotification = wsClient.getLastDataUpdate().getUpdate();
            assertThat(updatedNotification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + expectedStatus + ", " +
                    "severity: " + expectedSeverity.toString().toLowerCase() + ", deviceId: " + device.getId());
            assertThat(updatedNotification.getText()).isEqualTo("Status: " + expectedStatus + ", severity: " + expectedSeverity.toString().toLowerCase());

            wsClient.close();
        });
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger_clearRule() throws Exception {
        String notificationSubject = "${alarmSeverity} alarm '${alarmType}' is ${alarmStatus}";
        String notificationText = "${alarmId}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.WEB);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Web notification on any alarm");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setTriggerType(NotificationRuleTriggerType.ALARM);

        String alarmType = "myBoolIsTrue";
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "1234");

        AlarmNotificationRuleTriggerConfig triggerConfig = new AlarmNotificationRuleTriggerConfig();
        triggerConfig.setAlarmTypes(Set.of(alarmType));
        triggerConfig.setAlarmSeverities(null);
        triggerConfig.setNotifyOn(Set.of(AlarmAction.CREATED, AlarmAction.SEVERITY_CHANGED, AlarmAction.ACKNOWLEDGED));

        AlarmNotificationRuleTriggerConfig.ClearRule clearRule = new AlarmNotificationRuleTriggerConfig.ClearRule();
        clearRule.setAlarmStatuses(Set.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.UNACK));
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

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get() != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get();
        getWsClient().waitForUpdate(true);

        Notification notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("critical alarm '" + alarmType + "' is ACTIVE_UNACK");
        assertThat(notification.getInfo()).asInstanceOf(type(AlarmNotificationInfo.class))
                .extracting(AlarmNotificationInfo::getAlarmId).isEqualTo(alarm.getUuidId());

        await().atMost(10, TimeUnit.SECONDS).until(() -> findNotificationRequests(EntityType.ALARM).getTotalElements() == escalationTable.size());
        NotificationRequestInfo scheduledNotificationRequest = findNotificationRequests(EntityType.ALARM).getData().stream()
                .filter(NotificationRequest::isScheduled)
                .findFirst().orElse(null);
        assertThat(scheduledNotificationRequest).extracting(NotificationRequest::getInfo).isEqualTo(notification.getInfo());

        getWsClient().registerWaitForUpdate();
        alarmSubscriptionService.clearAlarm(tenantId, alarm.getId(), System.currentTimeMillis(), null);
        getWsClient().waitForUpdate(true);
        notification = getWsClient().getLastDataUpdate().getUpdate();
        assertThat(notification.getSubject()).isEqualTo("critical alarm '" + alarmType + "' is CLEARED_UNACK");

        assertThat(findNotificationRequests(EntityType.ALARM).getData()).filteredOn(NotificationRequest::isScheduled).isEmpty();
    }

    @Test
    public void testNotificationRuleProcessing_entitiesLimit() throws Exception {
        int limit = 5;
        updateDefaultTenantProfile(profileConfiguration -> {
            profileConfiguration.setMaxDevices(limit);
            profileConfiguration.setMaxAssets(limit);
            profileConfiguration.setMaxCustomers(limit);
            profileConfiguration.setMaxUsers(limit);
            profileConfiguration.setMaxDashboards(limit);
            profileConfiguration.setMaxRuleChains(limit);
        });

        EntitiesLimitNotificationRuleTriggerConfig triggerConfig = EntitiesLimitNotificationRuleTriggerConfig.builder()
                .entityTypes(null).threshold(0.8f)
                .build();
        loginSysAdmin();
        NotificationRule rule = createNotificationRule(triggerConfig, "${entityType}s limit will be reached soon",
                "${entityType}s usage: ${currentCount}/${limit} (${percents}%)", createNotificationTarget(tenantAdminUserId).getId());
        int threshold = (int) (limit * 0.8);
        loginTenantAdmin();

        checkNotificationAfter(() -> {
            for (int i = 1; i <= threshold; i++) {
                createDevice(i + "", i + "");
            }
        }, notification -> {
            assertThat(notification.getText()).isEqualTo("Devices usage: " + threshold + "/" + limit + " (80%)");
        });

        checkNotificationAfter(() -> {
            for (int i = 1; i <= threshold; i++) {
                Asset asset = new Asset();
                asset.setType("Test");
                asset.setName(i + "");
                doPost("/api/asset", asset);
            }
        }, notification -> {
            assertThat(notification.getText()).isEqualTo("Assets usage: " + threshold + "/" + limit + " (80%)");
        });

        checkNotificationAfter(() -> {
            long present = ruleChainService.countByTenantId(tenantId);
            for (int i = 1; i <= threshold - present; i++) {
                RuleChain ruleChain = new RuleChain();
                ruleChain.setName(i + "");
                ruleChain.setRoot(false);
                ruleChain.setDebugMode(false);
                ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
                RuleChainMetaData metaData = new RuleChainMetaData();
                metaData.setRuleChainId(ruleChain.getId());
                metaData.setNodes(List.of());
                doPost("/api/ruleChain/metadata", metaData);
            }
        }, notification -> {
            assertThat(notification.getText()).isEqualTo("Rule chains usage: " + threshold + "/" + limit + " (80%)");
        });

        triggerConfig.setThreshold(1.0f);
        rule.setTriggerConfig(triggerConfig);
        loginSysAdmin();
        saveNotificationRule(rule);
        loginTenantAdmin();

        checkNotificationAfter(() -> {
            createDevice(limit + "", limit + "");
        }, notification -> {
            assertThat(notification.getText()).isEqualTo("Devices usage: " + limit + "/" + limit + " (100%)");
        });
    }

    @Test
    public void testNotificationRuleInfo() throws Exception {
        NotificationDeliveryMethod[] deliveryMethods = {NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.EMAIL};
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

    @Test
    public void testNotificationRequestsPerRuleRateLimits() throws Exception {
        int notificationRequestsLimit = 10;
        updateDefaultTenantProfile(profileConfiguration -> {
            profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(notificationRequestsLimit + ":300");
        });

        NotificationRule rule = new NotificationRule();
        rule.setName("Device created");
        rule.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);
        NotificationTemplate template = createNotificationTemplate(NotificationType.ENTITY_ACTION,
                "Device created", "Device created", NotificationDeliveryMethod.WEB);
        rule.setTemplateId(template.getId());
        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        triggerConfig.setEntityTypes(Set.of(EntityType.DEVICE));
        triggerConfig.setCreated(true);
        rule.setTriggerConfig(triggerConfig);
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(NotificationRuleTriggerType.ENTITY_ACTION);
        recipientsConfig.setTargets(List.of(target.getUuidId()));
        rule.setRecipientsConfig(recipientsConfig);
        rule = saveNotificationRule(rule);

        getWsClient().subscribeForUnreadNotificationsCount().waitForReply();
        getWsClient().registerWaitForUpdate(notificationRequestsLimit);
        for (int i = 0; i < notificationRequestsLimit; i++) {
            String name = "device " + i;
            createDevice(name, name);
            TimeUnit.MILLISECONDS.sleep(200);
        }
        getWsClient().waitForUpdate(true);
        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isEqualTo(notificationRequestsLimit);

        for (int i = 0; i < 5; i++) {
            String name = "device " + (notificationRequestsLimit + i);
            createDevice(name, name);
        }

        boolean rateLimitExceeded = !rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, tenantId, rule.getId());
        assertThat(rateLimitExceeded).isTrue();

        TimeUnit.SECONDS.sleep(3);
        assertThat(getWsClient().getLastCountUpdate().getTotalUnreadCount()).isEqualTo(notificationRequestsLimit);
    }

    @Test
    public void testNotificationsDeduplication() throws Exception {
        loginSysAdmin();
        NewPlatformVersionNotificationRuleTriggerConfig triggerConfig = new NewPlatformVersionNotificationRuleTriggerConfig();
        createNotificationRule(triggerConfig, "Test", "Test", createNotificationTarget(tenantAdminUserId).getId());
        loginTenantAdmin();

        assertThat(getMyNotifications(false, 100)).size().isZero();
        for (int i = 1; i <= 10; i++) {
            notificationRuleProcessor.process(NewPlatformVersionTrigger.builder()
                    .updateInfo(new UpdateMessage(true, "test", "test",
                            "test", "test", "test"))
                    .build());
            TimeUnit.MILLISECONDS.sleep(300);
        }
        TimeUnit.SECONDS.sleep(5);
        assertThat(getMyNotifications(false, 100)).size().isOne();

        notificationRuleProcessor.process(NewPlatformVersionTrigger.builder()
                .updateInfo(new UpdateMessage(true, "CHANGED", "test",
                        "test", "test", "test"))
                .build());
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getMyNotifications(false, 100)).size().isEqualTo(2);
                });
    }

    private <R> R checkNotificationAfter(Callable<R> action, BiConsumer<Notification, R> check) throws Exception {
        if (getWsClient().getLastDataUpdate() == null) {
            getWsClient().subscribeForUnreadNotifications(10).waitForReply(true);
        }
        getWsClient().registerWaitForUpdate();
        R result = action.call();
        getWsClient().waitForUpdate(true);
        check.accept(getWsClient().getLastDataUpdate().getUpdate(), result);
        return result;
    }

    private void checkNotificationAfter(ThrowingRunnable action, Consumer<Notification> check) throws Exception {
        checkNotificationAfter(() -> {
            try {
                action.run();
                return null;
            } catch (Throwable e) {
                throw new Exception(e);
            }
        }, (notification, r) -> check.accept(notification));
    }

    private PageData<NotificationRequestInfo> findNotificationRequests(EntityType originatorType) {
        return notificationRequestService.findNotificationRequestsInfosByTenantIdAndOriginatorType(tenantId, originatorType, new PageLink(100));
    }

    private DeviceProfile createDeviceProfileWithAlarmRules(String alarmType) {
        DeviceProfile deviceProfile = createDeviceProfile("For notification rule test");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        createAlarmRule(tenantId, deviceProfile.getId(), alarmType);

        return deviceProfile;
    }

    private AlarmRule createAlarmRule(TenantId tenantId, DeviceProfileId deviceProfileId, String alarmType) {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType(alarmType);
        alarmRule.setName(alarmType);
        alarmRule.setEnabled(true);

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

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfileId);
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRule = doPost("/api/alarmRule", alarmRule, AlarmRule.class);
        return alarmRule;
    }

}
