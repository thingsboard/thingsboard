/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import java.util.Collection;

import static org.thingsboard.server.common.data.alarm.AlarmCommentSubType.SEVERITY_CHANGED;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultAlarmSubscriptionService extends AbstractSubscriptionService implements AlarmSubscriptionService {

    private final AlarmService alarmService;
    private final TbAlarmCommentService alarmCommentService;
    private final TbApiUsageReportClient apiUsageClient;
    private final TbApiUsageStateService apiUsageStateService;
    private final NotificationRuleProcessor notificationRuleProcessor;

    @Override
    protected String getExecutorPrefix() {
        return "alarm";
    }

    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request) {
        boolean creationEnabled = apiUsageStateService.getApiUsageState(request.getTenantId()).isAlarmCreationEnabled();
        var result = alarmService.createAlarm(request, creationEnabled);
        if (result.isCreated()) {
            apiUsageClient.report(request.getTenantId(), null, ApiUsageRecordKey.CREATED_ALARMS_COUNT);
        }
        return withWsCallback(request, result);
    }

    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        return withWsCallback(alarmService.updateAlarm(request));
    }

    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs) {
        return withWsCallback(alarmService.acknowledgeAlarm(tenantId, alarmId, ackTs));
    }

    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details) {
        return clearAlarm(tenantId, alarmId, clearTs, details, true);
    }

    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details, boolean pushEvent) {
        return withWsCallback(alarmService.clearAlarm(tenantId, alarmId, clearTs, details, pushEvent));
    }

    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTs) {
        return withWsCallback(alarmService.assignAlarm(tenantId, alarmId, assigneeId, assignTs));
    }

    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long assignTs) {
        return withWsCallback(alarmService.unassignAlarm(tenantId, alarmId, assignTs));
    }

    @Override
    public boolean deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        AlarmApiCallResult result = alarmService.delAlarm(tenantId, alarmId);
        onAlarmDeleted(result);
        return result.isSuccessful();
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmByIdAsync(tenantId, alarmId);
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmById(tenantId, alarmId);
    }

    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmInfoById(tenantId, alarmId);
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        return alarmService.findAlarms(tenantId, query);
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return alarmService.findCustomerAlarms(tenantId, customerId, query);
    }

    @Override
    public PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        return alarmService.findAlarmsV2(tenantId, query);
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        return alarmService.findCustomerAlarmsV2(tenantId, customerId, query);
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus, String assigneeId) {
        return alarmService.findHighestAlarmSeverity(tenantId, entityId, alarmSearchStatus, alarmStatus, assigneeId);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmService.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestActiveByOriginatorAndTypeAsync(tenantId, originator, type);
    }

    @Override
    public Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink) {
        return alarmService.findAlarmTypesByTenantId(tenantId, pageLink);
    }

    private void onAlarmUpdated(AlarmApiCallResult result) {
        wsCallBackExecutor.submit(() -> {
            AlarmInfo alarm = result.getAlarm();
            TenantId tenantId = alarm.getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                            subscriptionManagerService.onAlarmUpdate(tenantId, entityId, alarm, TbCallback.EMPTY);
                        }, () -> TbSubscriptionUtils.toAlarmUpdateProto(tenantId, entityId, alarm)
                );
            }
            notificationRuleProcessor.process(AlarmTrigger.builder()
                    .tenantId(tenantId)
                    .alarmUpdate(result)
                    .build());
        });
    }

    private void onAlarmDeleted(AlarmApiCallResult result) {
        wsCallBackExecutor.submit(() -> {
            AlarmInfo alarm = result.getAlarm();
            TenantId tenantId = alarm.getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                    subscriptionManagerService.onAlarmDeleted(tenantId, entityId, alarm, TbCallback.EMPTY);
                }, () -> {
                    return TbSubscriptionUtils.toAlarmDeletedProto(tenantId, entityId, alarm);
                });
            }
            notificationRuleProcessor.process(AlarmTrigger.builder()
                    .tenantId(tenantId)
                    .alarmUpdate(result)
                    .build());
        });
    }

    private class AlarmUpdateCallback implements FutureCallback<AlarmApiCallResult> {
        @Override
        public void onSuccess(@Nullable AlarmApiCallResult result) {
            onAlarmUpdated(result);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update alarm", t);
        }
    }

    private AlarmApiCallResult withWsCallback(AlarmApiCallResult result) {
        return withWsCallback(null, result);
    }

    private AlarmApiCallResult withWsCallback(AlarmModificationRequest request, AlarmApiCallResult result) {
        if (result.isSuccessful() && result.isModified()) {
            Futures.addCallback(Futures.immediateFuture(result), new AlarmUpdateCallback(), wsCallBackExecutor);
            if (result.isSeverityChanged()) {
                AlarmInfo alarm = result.getAlarm();
                AlarmComment.AlarmCommentBuilder alarmComment = AlarmComment.builder()
                        .alarmId(alarm.getId())
                        .type(AlarmCommentType.SYSTEM)
                        .comment(JacksonUtil.newObjectNode()
                                .put("text", String.format(SEVERITY_CHANGED.getText(), result.getOldSeverity(), alarm.getSeverity()))
                                .put("subtype", SEVERITY_CHANGED.name())
                                .put("oldSeverity", result.getOldSeverity().name())
                                .put("newSeverity", alarm.getSeverity().name()));
                if (request != null && request.getUserId() != null) {
                    alarmComment.userId(request.getUserId());
                }
                try {
                    alarmCommentService.saveAlarmComment(alarm, alarmComment.build(), null);
                } catch (ThingsboardException e) {
                    log.error("Failed to save alarm comment", e);
                }
            }
        }
        return result;
    }

}
