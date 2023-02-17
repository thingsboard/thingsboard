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
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.CreateOrUpdateActiveAlarmRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;


public interface AlarmService extends EntityDaoService {

    // New API, since 3.5.
    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    AlarmOperationResult createAlarm(CreateOrUpdateActiveAlarmRequest request);

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    AlarmOperationResult createAlarm(CreateOrUpdateActiveAlarmRequest request, boolean alarmCreationEnabled);

    /**
     * Designed to update existing alarm. Accepts only part of the alarm fields.
     *
     */
    AlarmOperationResult updateAlarm(AlarmUpdateRequest request);

    // Legacy API, before 3.5

    AlarmOperationResult createOrUpdateAlarm(Alarm alarm);

    AlarmOperationResult createOrUpdateAlarm(Alarm alarm, boolean alarmCreationEnabled);

    // Other API

    AlarmOperationResult deleteAlarm(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<AlarmOperationResult> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    ListenableFuture<AlarmOperationResult> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTs);

    AlarmOperationResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long ts);

    AlarmOperationResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long ts);

    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query);

    ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                           AlarmStatus alarmStatus, String assigneeId);

    ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                        AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId);
}
