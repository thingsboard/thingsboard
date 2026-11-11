// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DefaultAlarmSubscriptionServiceTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("2f5f24a2-fa4b-442e-b8c6-8ae37d09797a"));
    final DeviceId originator = DeviceId.fromString("6f67ba32-c7b5-4d68-96e9-be0f2b4c9c6d");
    final AlarmId alarmId = new AlarmId(UUID.fromString("2ce916b7-2b7a-4b62-8e0f-b7a51e9dd0f2"));

    @Mock
    AlarmService alarmService;
    @Mock
    TbAlarmCommentService alarmCommentService;
    @Mock
    TbApiUsageReportClient apiUsageClient;
    @Mock
    TbApiUsageStateService apiUsageStateService;
    @Mock
    NotificationRuleProcessor notificationRuleProcessor;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    SubscriptionManagerService subscriptionManagerService;

    ExecutorService wsCallBackExecutor;

    DefaultAlarmSubscriptionService alarmSubscriptionService;

    @BeforeEach
    void setup() {
        alarmSubscriptionService = new DefaultAlarmSubscriptionService(
                alarmService, alarmCommentService, apiUsageClient, apiUsageStateService, notificationRuleProcessor);
        alarmSubscriptionService.clusterService = clusterService;
        alarmSubscriptionService.partitionService = partitionService;
        alarmSubscriptionService.subscriptionManagerService = Optional.of(subscriptionManagerService);

        wsCallBackExecutor = MoreExecutors.newDirectExecutorService();
        alarmSubscriptionService.wsCallBackExecutor = wsCallBackExecutor;
    }

    @AfterEach
    void cleanup() {
        wsCallBackExecutor.shutdownNow();
    }

    @Test
    void deleteAlarmReturnsFalseAndSkipsDeletionCallbacksWhenDeletionIsNotSuccessful() {
        // GIVEN: the alarm was deleted concurrently (or the id is stale), so delAlarm reports no success
        given(alarmService.delAlarm(tenantId, originator, alarmId))
                .willReturn(AlarmApiCallResult.builder().successful(false).build());

        // WHEN
        boolean deleted = alarmSubscriptionService.deleteAlarm(tenantId, originator, alarmId);

        // THEN: false is returned and no deletion side effects fire
        assertThat(deleted).isFalse();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(subscriptionManagerService).shouldHaveNoInteractions();
        then(clusterService).shouldHaveNoInteractions();
        then(partitionService).shouldHaveNoInteractions();
    }

    @Test
    void deleteAlarmReturnsTrueAndProcessesDeletionCallbacksWhenDeletionIsSuccessful() {
        // GIVEN
        Alarm alarm = new Alarm(alarmId);
        alarm.setTenantId(tenantId);
        alarm.setOriginator(originator);
        given(alarmService.delAlarm(tenantId, originator, alarmId)).willReturn(AlarmApiCallResult.builder()
                .successful(true)
                .deleted(true)
                .alarm(new AlarmInfo(alarm))
                .propagatedEntitiesList(List.of())
                .build());

        // WHEN
        boolean deleted = alarmSubscriptionService.deleteAlarm(tenantId, originator, alarmId);

        // THEN
        assertThat(deleted).isTrue();
        then(notificationRuleProcessor).should().process(any(AlarmTrigger.class));
    }

    @Test
    void deleteAlarmNotifiesLocalSubscriptionManagerAndFiresAlarmTriggerWhenPropagatedEntityIsOnLocalPartition() {
        // GIVEN: a successful deletion that propagates to one entity
        Alarm alarm = new Alarm(alarmId);
        alarm.setTenantId(tenantId);
        alarm.setOriginator(originator);
        AlarmInfo alarmInfo = new AlarmInfo(alarm);
        AlarmApiCallResult result = AlarmApiCallResult.builder()
                .successful(true)
                .deleted(true)
                .alarm(alarmInfo)
                .propagatedEntitiesList(List.of(originator))
                .build();
        given(alarmService.delAlarm(tenantId, originator, alarmId)).willReturn(result);

        // AND: the propagated entity resolves to a partition owned by this node, so the fan-out stays local
        TopicPartitionInfo localPartition = new TopicPartitionInfo("tb_core", tenantId, 0, true);
        given(partitionService.resolve(ServiceType.TB_CORE, tenantId, originator)).willReturn(localPartition);
        alarmSubscriptionService.currentPartitions.add(localPartition);

        // WHEN
        boolean deleted = alarmSubscriptionService.deleteAlarm(tenantId, originator, alarmId);

        // THEN: the local subscription manager is notified directly and nothing is pushed to a remote core
        assertThat(deleted).isTrue();
        then(subscriptionManagerService).should().onAlarmDeleted(tenantId, originator, alarmInfo, TbCallback.EMPTY);
        then(clusterService).shouldHaveNoInteractions();

        // AND: the AlarmTrigger carries the tenant and the deletion result
        ArgumentCaptor<AlarmTrigger> alarmTriggerCaptor = ArgumentCaptor.forClass(AlarmTrigger.class);
        then(notificationRuleProcessor).should().process(alarmTriggerCaptor.capture());
        AlarmTrigger alarmTrigger = alarmTriggerCaptor.getValue();
        assertThat(alarmTrigger.getTenantId()).isEqualTo(tenantId);
        assertThat(alarmTrigger.getAlarmUpdate()).isEqualTo(result);
    }

}
