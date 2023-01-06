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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
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
import org.thingsboard.server.common.data.notification.AlarmOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.NotificationEscalation;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class NotificationRuleApiTest extends AbstractNotificationApiTest {

    @SpyBean
    private AlarmSubscriptionService alarmSubscriptionService;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testNotificationRuleProcessing() throws Exception {
        NotificationDeliveryMethod[] deliveryMethods = {NotificationDeliveryMethod.WEBSOCKET, NotificationDeliveryMethod.EMAIL};
        NotificationTemplate notificationTemplate = createNotificationTemplate(NotificationType.ALARM, "New alarm", "NEW ALARM ${alarmType}", deliveryMethods);

        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setTenantId(tenantId);
        notificationRule.setName("Test rule for my alarms");
        notificationRule.setTemplateId(notificationTemplate.getId());
        notificationRule.setDeliveryMethods(List.of(deliveryMethods));
        NotificationRuleConfig config = new NotificationRuleConfig();

        List<NotificationEscalation> escalations = new ArrayList<>();
        Map<Integer, NotificationApiWsClient> clients = new HashMap<>();
        for (int delay = 0; delay <= 5; delay++) {
            Pair<User, NotificationApiWsClient> userAndClient = createUserAndConnectWsClient(Authority.TENANT_ADMIN);
            NotificationTarget notificationTarget = createNotificationTarget(userAndClient.getFirst().getId());

            NotificationEscalation escalation = new NotificationEscalation();
            escalation.setDelayInSec(delay);
            escalation.setNotificationTargets(List.of(notificationTarget.getId()));
            escalations.add(escalation);
            clients.put(delay, userAndClient.getSecond());
        }

        config.setEscalations(escalations);
        notificationRule.setConfiguration(config);
        notificationRule = saveNotificationRule(notificationRule);
        String alarmType = "boolIsTrue";
        DeviceProfile deviceProfile = createDeviceProfileWithAlarmRules(notificationRule.getId(), alarmType);
        Device device = createDevice("Device 1", deviceProfile.getName(), "1234");

        clients.values().forEach(wsClient -> {
            wsClient.subscribeForUnreadNotifications(10);
            wsClient.waitForReply();
            wsClient.registerWaitForUpdate();
        });

        JsonNode attr = JacksonUtil.newObjectNode()
                .set("bool", BooleanNode.TRUE);
        doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attr);

        verify(alarmSubscriptionService, timeout(2000)).createOrUpdateAlarm(argThat(alarm -> alarm.getType().equals(alarmType)));
        Alarm alarm = alarmSubscriptionService.findLatestByOriginatorAndType(tenantId, device.getId(), alarmType).get();
        assertThat(alarm.getNotificationRuleId()).isEqualTo(notificationRule.getId());
        long ts = System.currentTimeMillis();

        await().atMost(7, TimeUnit.SECONDS)
                .until(() -> clients.values().stream().allMatch(client -> client.getLastDataUpdate() != null));
        clients.forEach((expectedDelay, wsClient) -> {
            Notification notification = wsClient.getLastDataUpdate().getUpdate();
            double actualDelay = (double) (notification.getCreatedTime() - ts) / 1000;
            assertThat(actualDelay).isCloseTo(expectedDelay, offset(0.5));

            assertThat(notification.getText()).isEqualTo("NEW ALARM " + alarm.getType());
            assertThat(notification.getType()).isEqualTo(NotificationType.ALARM);
            assertThat(notification.getSubject()).isEqualTo("New alarm");
            assertThat(notification.getInfo()).isInstanceOf(AlarmOriginatedNotificationInfo.class);
            AlarmOriginatedNotificationInfo info = (AlarmOriginatedNotificationInfo) notification.getInfo();
            assertThat(info.getAlarmId()).isEqualTo(alarm.getId());
            assertThat(info.getAlarmType()).isEqualTo(alarm.getType());
            assertThat(info.getAlarmSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(info.getAlarmStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        clients.values().forEach(wsClient -> wsClient.registerWaitForUpdate());
        alarmSubscriptionService.ackAlarm(tenantId, alarm.getId(), System.currentTimeMillis());
        clients.values().forEach(wsClient -> {
            wsClient.waitForUpdate(true);
            Notification updatedNotification = wsClient.getLastDataUpdate().getUpdate();
            assertThat(((AlarmOriginatedNotificationInfo) updatedNotification.getInfo()).getAlarmStatus())
                    .isEqualTo(AlarmStatus.ACTIVE_ACK);

            wsClient.close();
        });
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

}
