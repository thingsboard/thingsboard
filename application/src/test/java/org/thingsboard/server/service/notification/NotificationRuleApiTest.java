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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.SystemUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.ResourcesShortageTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.ResourcesShortageTrigger.Resource;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ResourcesShortageNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedTenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.notification.DefaultNotificationDeduplicationService;
import org.thingsboard.server.service.notification.rule.cache.DefaultNotificationRulesCache;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.system.DefaultSystemInfoService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig.Action.ASSIGNED;
import static org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig.Action.UNASSIGNED;
import static org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent.ACTIVE;
import static org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent.INACTIVE;

@DaoSqlTest
@TestPropertySource(properties = {
        "transport.http.enabled=true",
        "notification_system.rules.deduplication_durations=RATE_LIMITS:10000",
        "edges.enabled=true"
})
public class NotificationRuleApiTest extends AbstractNotificationApiTest {

    @MockitoSpyBean
    private AlarmSubscriptionService alarmSubscriptionService;
    @Autowired
    private DefaultSystemInfoService systemInfoService;
    @Autowired
    private NotificationRequestService notificationRequestService;
    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;
    @Autowired
    private DefaultNotificationRulesCache notificationRulesCache;
    @Autowired
    private DeviceStateService deviceStateService;

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
                "severity: ${alarmSeverity}, deviceId: ${alarmOriginatorId}, details: ${details.data}.";
        String notificationText = "Status: ${alarmStatus}, severity: ${alarmSeverity}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.WEB);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Web notification on any alarm");
        notificationRule.setEnabled(true);
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
        saveNotificationRule(notificationRule);


        String alarmType = "myBoolIsTrue";
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "label", "1234");

        clients.values().forEach(wsClient -> {
            wsClient.subscribeForUnreadNotifications(10).waitForReply(true);
            wsClient.registerWaitForUpdate();
        });

        JsonNode attr = JacksonUtil.newObjectNode()
                .set("createAlarm", BooleanNode.TRUE);
        postAttributes(device.getId(), AttributeScope.SERVER_SCOPE, attr.toString());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType) != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType);

        long ts = System.currentTimeMillis();
        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> clients.values().stream().allMatch(client -> client.getLastDataUpdate() != null));
        clients.forEach((expectedDelay, wsClient) -> {
            Notification notification = wsClient.getLastDataUpdate().getUpdate();
            double actualDelay = (double) (notification.getCreatedTime() - ts) / 1000;
            assertThat(actualDelay).isCloseTo(expectedDelay, offset(2.0));

            assertThat(notification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + AlarmStatus.ACTIVE_UNACK + ", " +
                    "severity: " + AlarmSeverity.CRITICAL.toString().toLowerCase() + ", deviceId: " + device.getId() + ", details: attribute is true.");
            assertThat(notification.getText()).isEqualTo("Status: " + AlarmStatus.ACTIVE_UNACK + ", severity: " + AlarmSeverity.CRITICAL.toString().toLowerCase());

            assertThat(notification.getType()).isEqualTo(NotificationType.ALARM);
            assertThat(notification.getInfo()).isInstanceOf(AlarmNotificationInfo.class);
            AlarmNotificationInfo info = (AlarmNotificationInfo) notification.getInfo();
            assertThat(info.getAlarmId()).isEqualTo(alarm.getUuidId());
            assertThat(info.getAlarmType()).isEqualTo(alarmType);
            assertThat(info.getAlarmSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(info.getAlarmStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        clients.values().forEach(TbTestWebSocketClient::registerWaitForUpdate);
        alarmSubscriptionService.acknowledgeAlarm(tenantId, alarm.getId(), System.currentTimeMillis());
        AlarmStatus expectedStatus = AlarmStatus.ACTIVE_ACK;
        AlarmSeverity expectedSeverity = AlarmSeverity.CRITICAL;
        clients.values().forEach(wsClient -> {
            wsClient.waitForUpdate(true);
            Notification updatedNotification = wsClient.getLastDataUpdate().getUpdate();
            assertThat(updatedNotification.getSubject()).isEqualTo("Alarm type: " + alarmType + ", status: " + expectedStatus + ", " +
                    "severity: " + expectedSeverity.toString().toLowerCase() + ", deviceId: " + device.getId() + ", details: attribute is true.");
            assertThat(updatedNotification.getText()).isEqualTo("Status: " + expectedStatus + ", severity: " + expectedSeverity.toString().toLowerCase());

            wsClient.close();
        });
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger_createViaRestApi() throws Exception {
        Device device = createDevice("Device with alarm", "233");
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        defaultNotifications.create(tenantId, DefaultNotifications.newAlarm, target.getId());
        notificationRulesCache.evict(tenantId);

        Alarm alarm = new Alarm();
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setType("testAlarm");
        alarm.setOriginator(device.getId());
        alarm = doPost("/api/alarm", alarm, Alarm.class);

        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = getMyNotifications(false, 10);
                    assertThat(notifications).singleElement().matches(notification -> {
                        return notification.getType() == NotificationType.ALARM &&
                                notification.getSubject().equals("New alarm 'testAlarm'");
                    });
                });
    }

    @Test
    public void testNotificationRuleProcessing_alarmTrigger_clearRule() throws Exception {
        String notificationSubject = "${alarmSeverity} alarm '${alarmType}' is ${alarmStatus}";
        String notificationText = "${alarmId}";
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, notificationSubject, notificationText, NotificationDeliveryMethod.WEB);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setName("Web notification on any alarm");
        notificationRule.setEnabled(true);
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
                .set("createAlarm", BooleanNode.TRUE);
        postAttributes(device.getId(), AttributeScope.SERVER_SCOPE, attr.toString());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType) != null);
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType);
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

        alarmSubscriptionService.clearAlarm(tenantId, alarm.getId(), System.currentTimeMillis(), null);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(findNotificationRequests(EntityType.ALARM).getData()).filteredOn(NotificationRequest::isScheduled).isEmpty();
        });
    }

    @Test
    public void testNotificationRuleProcessing_entitiesLimit() throws Exception {
        int limit = 5;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setMaxDevices(limit);
            profileConfiguration.setMaxAssets(limit);
            profileConfiguration.setMaxCustomers(limit);
            profileConfiguration.setMaxUsers(limit);
            profileConfiguration.setMaxDashboards(limit);
            profileConfiguration.setMaxRuleChains(limit);
            profileConfiguration.setMaxEdges(limit);
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

        checkNotificationAfter(() -> {
            for (int i = 1; i <= threshold; i++) {
                Edge edge = new Edge();
                edge.setName(i + "");
                edge.setType("default");
                edge.setSecret("secret_" + i);
                edge.setRoutingKey("routingKey_" + i);
                doPost("/api/edge", edge);
            }
        }, notification -> {
            assertThat(notification.getText()).isEqualTo("Edges usage: " + threshold + "/" + limit + " (80%)");
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
    public void testNotificationRuleProcessing_exceededRateLimits() throws Exception {
        loginSysAdmin();
        NotificationTarget sysadmins = createNotificationTarget(new SystemAdministratorsFilter());
        NotificationTarget affectedTenantAdmins = createNotificationTarget(new AffectedTenantAdministratorsFilter());
        defaultNotifications.create(TenantId.SYS_TENANT_ID, DefaultNotifications.exceededRateLimitsForSysadmin, sysadmins.getId());
        defaultNotifications.create(TenantId.SYS_TENANT_ID, DefaultNotifications.exceededRateLimits, affectedTenantAdmins.getId());
        defaultNotifications.create(TenantId.SYS_TENANT_ID, DefaultNotifications.exceededPerEntityRateLimits, affectedTenantAdmins.getId());
        notificationRulesCache.evict(TenantId.SYS_TENANT_ID);

        int n = 10;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setTenantEntityExportRateLimit(n + ":600");
            profileConfiguration.setCustomerServerRestLimitsConfiguration(n + ":600");
            profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(n + ":600");
            profileConfiguration.setTransportDeviceTelemetryMsgRateLimit(n + ":600");
        });
        loginTenantAdmin();
        NotificationRule rule = createNotificationRule(AlarmCommentNotificationRuleTriggerConfig.builder()
                .alarmTypes(Set.of("weklfjkwefa"))
                .build(), "Test", "Test", createNotificationTarget(tenantAdminUserId).getId());
        for (int i = 1; i <= n * 2; i++) {
            rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, tenantId);
            rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_CUSTOMER, tenantId, customerId);
            rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, tenantId, rule.getId());
            Thread.sleep(100);
        }

        loginTenantAdmin();
        List<Notification> notifications = await().atMost(15, TimeUnit.SECONDS)
                .until(() -> getMyNotifications(true, 10).stream()
                        .filter(notification -> notification.getType() == NotificationType.RATE_LIMITS)
                        .collect(Collectors.toList()), list -> list.size() == 3);
        assertThat(notifications).allSatisfy(notification -> {
            assertThat(notification.getSubject()).isEqualTo("Rate limits exceeded");
        });
        assertThat(notifications).anySatisfy(notification -> {
            assertThat(notification.getText()).isEqualTo("Rate limits for entity version creation exceeded");
        });
        assertThat(notifications).anySatisfy(notification -> {
            assertThat(notification.getText()).isEqualTo("Rate limits for REST API requests per customer " +
                    "exceeded for 'Customer'");
        });
        assertThat(notifications).anySatisfy(notification -> {
            assertThat(notification.getText()).isEqualTo("Rate limits for notification requests " +
                    "per rule exceeded for '" + rule.getName() + "'");
        });

        loginSysAdmin();
        notifications = await().atMost(15, TimeUnit.SECONDS)
                .until(() -> getMyNotifications(true, 10).stream()
                        .filter(notification -> notification.getType() == NotificationType.RATE_LIMITS)
                        .collect(Collectors.toList()), list -> list.size() == 1);
        assertThat(notifications).singleElement().satisfies(notification -> {
            assertThat(notification.getSubject()).isEqualTo("Rate limits exceeded for tenant " + TEST_TENANT_NAME);
            assertThat(notification.getText()).isEqualTo("Rate limits for entity version creation exceeded");
        });
    }

    @Test
    public void testNotificationRuleProcessing_alarmAssignment() throws Exception {
        AlarmAssignmentNotificationRuleTriggerConfig triggerConfig = AlarmAssignmentNotificationRuleTriggerConfig.builder()
                .alarmTypes(Set.of("test"))
                .notifyOn(Set.of(ASSIGNED, UNASSIGNED))
                .build();
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        String template = "${userEmail} ${action} alarm on ${alarmOriginatorEntityType} '${alarmOriginatorName}' with label '${alarmOriginatorLabel}'. Assignee: ${assigneeEmail}";
        createNotificationRule(triggerConfig, "Test", template, target.getId());

        Device device = createDevice("Device A", "default", "test", "123");
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .cleared(false)
                .acknowledged(false)
                .severity(AlarmSeverity.CRITICAL)
                .type("test")
                .startTs(System.currentTimeMillis())
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        AlarmId alarmId = alarm.getId();

        checkNotificationAfter(() -> {
            doPost("/api/alarm/" + alarmId + "/assign/" + tenantAdminUserId).andExpect(status().isOk());
        }, notification -> {
            assertThat(notification.getText()).isEqualTo(
                    TENANT_ADMIN_EMAIL + " assigned alarm on Device 'Device A' with label 'test'. Assignee: " + TENANT_ADMIN_EMAIL
            );
        });

        checkNotificationAfter(() -> {
            doDelete("/api/alarm/" + alarmId + "/assign").andExpect(status().isOk());
        }, notification -> {
            assertThat(notification.getText()).isEqualTo(
                    TENANT_ADMIN_EMAIL + " unassigned alarm on Device 'Device A' with label 'test'. Assignee: "
            );
        });
    }

    @Test
    public void testNotificationRuleProcessing_alarmComment() throws Exception {
        AlarmCommentNotificationRuleTriggerConfig triggerConfig = AlarmCommentNotificationRuleTriggerConfig.builder()
                .alarmTypes(Set.of("test"))
                .onlyUserComments(true)
                .notifyOnCommentUpdate(true)
                .build();
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        String template = "${userEmail} ${action} comment on alarm ${alarmType}: ${comment}";
        createNotificationRule(triggerConfig, "Test", template, target.getId());

        Device device = createDevice("Device A", "123");
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .cleared(false)
                .acknowledged(false)
                .severity(AlarmSeverity.CRITICAL)
                .type("test")
                .startTs(System.currentTimeMillis())
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        AlarmId alarmId = alarm.getId();

        AlarmComment comment = checkNotificationAfter(() -> {
            return doPost("/api/alarm/" + alarmId + "/comment",
                    AlarmComment.builder()
                            .type(AlarmCommentType.OTHER)
                            .comment(JacksonUtil.newObjectNode()
                                    .put("text", "this is bad"))
                            .build(), AlarmComment.class);
        }, (notification, r) -> {
            assertThat(notification.getText()).isEqualTo(
                    TENANT_ADMIN_EMAIL + " added comment on alarm test: this is bad"
            );
        });

        checkNotificationAfter(() -> {
            ((ObjectNode) comment.getComment()).put("text", "this is very bad");
            doPost("/api/alarm/" + alarmId + "/comment", comment);
        }, notification -> {
            assertThat(notification.getText()).isEqualTo(
                    TENANT_ADMIN_EMAIL + " updated comment on alarm test: this is very bad"
            );
        });
    }

    @Test
    public void testNotificationRuleProcessing_deviceActivity() throws Exception {
        DeviceActivityNotificationRuleTriggerConfig triggerConfig = DeviceActivityNotificationRuleTriggerConfig.builder()
                .notifyOn(Set.of(ACTIVE, INACTIVE))
                .build();
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        String template = "Device ${deviceName} (${deviceLabel}) of type ${deviceType} is now ${eventType}";
        createNotificationRule(triggerConfig, "Test", template, target.getId());

        Device device = new Device();
        device.setName("A");
        device.setLabel("Test Device A");
        device.setType("test");
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        device = doPost("/api/device", device, Device.class);
        DeviceId deviceId = device.getId();

        checkNotificationAfter(() -> {
            deviceStateService.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        }, notification -> {
            assertThat(notification.getText()).isEqualTo(
                    "Device A (Test Device A) of type test is now active"
            );
        });
    }

    @Test
    public void testNotificationRuleInfo() throws Exception {
        NotificationDeliveryMethod[] deliveryMethods = {NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.EMAIL};
        NotificationTemplate template = createNotificationTemplate(NotificationType.ENTITY_ACTION, "Subject", "Text", deliveryMethods);

        NotificationRule rule = new NotificationRule();
        rule.setName("Test");
        rule.setEnabled(true);
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
        assertThat(ruleInfo.getId()).isEqualTo(rule.getId());
        assertThat(ruleInfo.getTemplateName()).isEqualTo(template.getName());
        assertThat(ruleInfo.getDeliveryMethods()).containsOnly(deliveryMethods);
    }

    @Test
    public void testNotificationRequestsPerRuleRateLimits() throws Exception {
        int notificationRequestsLimit = 10;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(notificationRequestsLimit + ":300");
        });

        NotificationRule rule = new NotificationRule();
        rule.setName("Device created");
        rule.setEnabled(true);
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
    public void testNotificationsDeduplication_newPlatformVersion() throws Exception {
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

    @Test
    public void testNotificationsDeduplication_exceededRateLimits() throws Exception {
        RateLimitsNotificationRuleTriggerConfig triggerConfig = new RateLimitsNotificationRuleTriggerConfig();
        triggerConfig.setApis(Set.of(LimitedApi.ENTITY_EXPORT, LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE));

        loginSysAdmin();
        NotificationTarget target = createNotificationTarget(tenantAdminUserId);
        NotificationRule rule = createNotificationRule(triggerConfig, "Test 1", "Test", target.getId());

        int n = 5;
        updateDefaultTenantProfile(profileConfiguration -> {
            profileConfiguration.getProfileConfiguration().get().setTenantEntityExportRateLimit(n + ":600");
            profileConfiguration.getProfileConfiguration().get().setTransportDeviceTelemetryMsgRateLimit(n + ":800");
        });

        RateLimitsTrigger expectedTrigger = RateLimitsTrigger.builder()
                .tenantId(tenantId)
                .api(LimitedApi.ENTITY_EXPORT)
                .limitLevel(tenantId)
                .build();
        assertThat(DefaultNotificationDeduplicationService.getDeduplicationKey(expectedTrigger, rule))
                .isEqualTo("RATE_LIMITS:TENANT:" + tenantId + ":ENTITY_EXPORT_" +
                        target.getId() + ":ENTITY_EXPORT,TRANSPORT_MESSAGES_PER_DEVICE");

        loginTenantAdmin();
        getWsClient().subscribeForUnreadNotifications(10).waitForReply();
        getWsClient().registerWaitForUpdate(2);
        Device device = createDevice("Test", "Test");
        for (int i = 1; i <= n + 1; i++) {
            rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, tenantId);
            doPost("/api/v1/" + device.getName() + "/telemetry", "{\"dp1\":123}", String.class);
        }
        int expectedNotificationsCount1 = 2;
        getWsClient().waitForUpdate(true);
        List<Notification> notifications1 = getMyNotifications(true, 10);
        assertThat(notifications1).size().isEqualTo(expectedNotificationsCount1);
        assertThat(notifications1)
                .anyMatch(notification -> ((RateLimitsNotificationInfo) notification.getInfo()).getApi() == LimitedApi.ENTITY_EXPORT)
                .anyMatch(notification -> ((RateLimitsNotificationInfo) notification.getInfo()).getApi() == LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE);

        getWsClient().registerWaitForUpdate(2);
        for (int i = 0; i < 10; i++) {
            rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, tenantId);
            doPost("/api/v1/" + device.getName() + "/telemetry", "{\"dp1\":123}", String.class);
        }
        assertThat(getWsClient().waitForUpdate(5000)).isNull();

        int deduplicationDuration = 10000; // configured in TestPropertySource above
        await().atLeast(2, TimeUnit.SECONDS)
                .atMost(deduplicationDuration, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, tenantId);
                    doPost("/api/v1/" + device.getName() + "/telemetry", "{\"dp1\":123}", String.class);

                    Map<LimitedApi, Long> notifications2 = getMyNotifications(true, 10).stream()
                            .map(notification -> (RateLimitsNotificationInfo) notification.getInfo())
                            .collect(Collectors.groupingBy(RateLimitsNotificationInfo::getApi, Collectors.counting()));
                    assertThat(notifications2.get(LimitedApi.ENTITY_EXPORT)).isEqualTo(2);
                    assertThat(notifications2.get(LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE)).isEqualTo(2);
                });
    }

    @Test
    public void testNotificationRuleProcessing_resourcesShortage() throws Exception {
        loginSysAdmin();
        ResourcesShortageNotificationRuleTriggerConfig triggerConfig = ResourcesShortageNotificationRuleTriggerConfig.builder()
                .ramThreshold(0.01f)
                .cpuThreshold(1f)
                .storageThreshold(1f)
                .build();
        createNotificationRule(triggerConfig, "Warning: ${resource} shortage", "${resource} shortage", createNotificationTarget(tenantAdminUserId).getId());
        loginTenantAdmin();

        // Mock SystemUtil to return 15% memory usage (exceeds 1% threshold)
        try (MockedStatic<SystemUtil> mockedSystemUtil = mockStatic(SystemUtil.class)) {
            mockedSystemUtil.when(SystemUtil::getMemoryUsage).thenReturn(Optional.of(15));

            Method method = DefaultSystemInfoService.class.getDeclaredMethod("saveCurrentMonolithSystemInfo");
            method.setAccessible(true);
            method.invoke(systemInfoService);
        }

        await().atMost(10, TimeUnit.SECONDS).until(() -> getMyNotifications(false, 100).size() == 1);
        Notification notification = getMyNotifications(false, 100).get(0);
        assertThat(notification.getSubject()).isEqualTo("Warning: RAM shortage");
        assertThat(notification.getText()).isEqualTo("RAM shortage");
    }

    @Test
    public void testNotificationsDeduplication_resourcesShortage() throws Exception {
        loginSysAdmin();
        ResourcesShortageNotificationRuleTriggerConfig triggerConfig = ResourcesShortageNotificationRuleTriggerConfig.builder()
                .ramThreshold(0.01f)
                .cpuThreshold(1f)
                .storageThreshold(1f)
                .build();
        createNotificationRule(triggerConfig, "Warning: ${resource} shortage", "${resource} shortage", createNotificationTarget(tenantAdminUserId).getId());
        loginTenantAdmin();

        assertThat(getMyNotifications(false, 100)).size().isZero();
        for (int i = 0; i < 10; i++) {
            notificationRuleProcessor.process(ResourcesShortageTrigger.builder()
                    .resource(Resource.RAM)
                    .usage(15L)
                    .serviceType("serviceType")
                    .serviceId("serviceId")
                    .build());
            TimeUnit.MILLISECONDS.sleep(300);
        }
        await().atMost(10, TimeUnit.SECONDS).until(() -> getMyNotifications(false, 100).size() == 1);
        Notification notification = getMyNotifications(false, 100).get(0);
        assertThat(notification.getSubject()).isEqualTo("Warning: RAM shortage");
        assertThat(notification.getText()).isEqualTo("RAM shortage");

        // deduplication is 1 minute, no new message is exp
        notificationRuleProcessor.process(ResourcesShortageTrigger.builder()
                .resource(Resource.RAM)
                .usage(5L)
                .serviceType("serviceType")
                .serviceId("serviceId")
                .build());
        await("").atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(getMyNotifications(false, 100)).size().isOne());
    }

    @Test
    public void testNotificationsResourcesShortage_whenThresholdChangeToMatchingFilter_thenSendNotification() throws Exception {
        loginSysAdmin();
        ResourcesShortageNotificationRuleTriggerConfig triggerConfig = ResourcesShortageNotificationRuleTriggerConfig.builder()
                .ramThreshold(1f)
                .cpuThreshold(1f)
                .storageThreshold(1f)
                .build();
        NotificationRule rule = createNotificationRule(triggerConfig, "Warning: ${resource} shortage", "${resource} shortage", createNotificationTarget(tenantAdminUserId).getId());
        loginTenantAdmin();

        // Mock SystemUtil to return 15% usages (not exceeds 100% threshold)
        Method method;
        try (MockedStatic<SystemUtil> mockedSystemUtil = mockStatic(SystemUtil.class)) {
            mockedSystemUtil.when(SystemUtil::getMemoryUsage).thenReturn(Optional.of(15));
            mockedSystemUtil.when(SystemUtil::getCpuUsage).thenReturn(Optional.of(15));
            mockedSystemUtil.when(SystemUtil::getDiscSpaceUsage).thenReturn(Optional.of(15L));

            method = DefaultSystemInfoService.class.getDeclaredMethod("saveCurrentMonolithSystemInfo");
            method.setAccessible(true);
            method.invoke(systemInfoService);
        }

        TimeUnit.SECONDS.sleep(5);
        assertThat(getMyNotifications(false, 100)).size().isZero();

        loginSysAdmin();
        triggerConfig = ResourcesShortageNotificationRuleTriggerConfig.builder()
                .ramThreshold(0.01f)
                .cpuThreshold(1f)
                .storageThreshold(1f)
                .build();
        rule.setTriggerConfig(triggerConfig);
        saveNotificationRule(rule);
        loginTenantAdmin();

        method.invoke(systemInfoService);

        await().atMost(10, TimeUnit.SECONDS).until(() -> getMyNotifications(false, 100).size() == 1);
        Notification notification = getMyNotifications(false, 100).get(0);
        assertThat(notification.getSubject()).isEqualTo("Warning: RAM shortage");
        assertThat(notification.getText()).isEqualTo("RAM shortage");
    }

    @Test
    public void testNotificationRuleDisabling() throws Exception {
        EntityActionNotificationRuleTriggerConfig triggerConfig = new EntityActionNotificationRuleTriggerConfig();
        triggerConfig.setEntityTypes(Set.of(EntityType.DEVICE));
        triggerConfig.setCreated(true);
        NotificationRule rule = createNotificationRule(triggerConfig, "Created", "Created", createNotificationTarget(tenantAdminUserId).getId());
        notificationRulesCache.evict(tenantId);

        assertThat(getMyNotifications(false, 100)).size().isZero();
        createDevice("Device 1", "default", "111");
        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getMyNotifications(false, 100)).size().isEqualTo(1);
                });

        rule.setEnabled(false);
        saveNotificationRule(rule);
        notificationRulesCache.evict(tenantId);

        createDevice("Device 2", "default", "222");
        TimeUnit.SECONDS.sleep(5);
        assertThat(getMyNotifications(false, 100)).as("No new notifications arrived").size().isEqualTo(1);

        rule.setEnabled(true);
        saveNotificationRule(rule);
        notificationRulesCache.evict(tenantId);

        createDevice("Device 3", "default", "333");
        await().atMost(TIMEOUT, TimeUnit.SECONDS)
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

        CalculatedField alarmCf = new CalculatedField();
        alarmCf.setType(CalculatedFieldType.ALARM);
        alarmCf.setEntityId(deviceProfile.getId());
        alarmCf.setName(alarmType);
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("createAlarm", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        configuration.setArguments(Map.of("createAlarm", argument));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("attribute is ${createAlarm}");
        SimpleAlarmCondition condition = new SimpleAlarmCondition();
        TbelAlarmConditionExpression expression = new TbelAlarmConditionExpression();
        expression.setExpression("return createAlarm == true;");
        condition.setExpression(expression);
        alarmRule.setCondition(condition);
        configuration.setCreateRules(Map.of(
                AlarmSeverity.CRITICAL, alarmRule
        ));
        alarmCf.setConfiguration(configuration);
        saveCalculatedField(alarmCf);
        return deviceProfile;
    }

}
