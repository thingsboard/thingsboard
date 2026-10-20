// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RuleEngineAlarmService {

    /*
     *  New API, since 3.5.
     */

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request);

    /**
     * Designed to update existing alarm. Accepts only part of the alarm fields.
     */
    AlarmApiCallResult updateAlarm(AlarmUpdateRequest request);

    /*
     * Since 4.3.1.4, acknowledgeAlarm/clearAlarm/assignAlarm/unassignAlarm/deleteAlarm require the alarm originator: the
     * alarm table is sharded by originator, and the originator routes each operation to the single owning shard. The
     * old by-id forms were removed on purpose -- an intentional source-compatibility break for custom rule nodes
     * (by-id lookups fan out across every shard, which is exactly what the sharding eliminated). The alarm, and thus
     * its originator, is almost always in hand at the call site; if it truly is not, resolve it via findAlarmById
     * first.
     */

    AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long ackTs);

    AlarmApiCallResult clearAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long clearTs, JsonNode details);

    AlarmApiCallResult clearAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long clearTs, JsonNode details, boolean pushEvent);

    AlarmApiCallResult assignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, UserId assigneeId, long assignTs);

    AlarmApiCallResult unassignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long assignTs);

    // Other API
    boolean deleteAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type);

    Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    default ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return Futures.immediateFuture(findAlarmInfoById(tenantId, alarmId));
    }

    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query);

    PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus, String assigneeId);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions, AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    List<Long> findAlarmCounts(TenantId tenantId, AlarmQuery query, List<AlarmFilter> filters);

    Set<EntityId> getPropagationEntityIds(Alarm alarm);

    Set<EntityId> getPropagationEntityIds(Alarm alarm, List<EntityType> types);

    PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink);

}
