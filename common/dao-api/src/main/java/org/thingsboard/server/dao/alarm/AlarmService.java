// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
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
import org.thingsboard.server.common.data.alarm.AlarmRef;
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
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.OriginatorAlarmFilter;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AlarmService extends EntityDaoService {

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request);

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled);

    /**
     * Designed to update existing alarm. Accepts only part of the alarm fields.
     */
    AlarmApiCallResult updateAlarm(AlarmUpdateRequest request);

    AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long ackTs);

    AlarmApiCallResult clearAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long clearTs, JsonNode details, boolean pushEvent);

    AlarmApiCallResult assignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, UserId assigneeId, long ts);

    AlarmApiCallResult unassignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long ts);

    AlarmApiCallResult delAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId);

    AlarmApiCallResult delAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, boolean checkAndDeleteAlarmType);

    /**
     * Clears the assignee of every alarm assigned to the given user in a single bulk UPDATE. NOT a batched
     * {@link #unassignAlarm}: it deliberately skips all per-alarm side effects -- the UNASSIGNED_FROM_DELETED_USER
     * system comment, the ALARM_UNASSIGNED audit log/notification and the resulting rule-engine message (so no
     * push-to-edge propagation), and websocket subscription updates. See BaseAlarmService.unassignAlarmsByAssignee
     * for why dropping them is acceptable on the deleted-user cleanup path.
     */
    int unassignAlarmsByAssignee(TenantId tenantId, UserId assigneeId, long unassignTs);

    void delAlarmTypes(TenantId tenantId, Set<String> types);

    // Other API
    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query);

    PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query);

    List<Long> findAlarmCounts(TenantId tenantId, AlarmQuery query, List<AlarmFilter> filters);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                           AlarmStatus alarmStatus, String assigneeId);

    Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions,
                                                        AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    Set<EntityId> getPropagationEntityIds(Alarm alarm);

    Set<EntityId> getPropagationEntityIds(Alarm alarm, List<EntityType> types);

    List<AlarmRef> findAlarmRefsByAssigneeId(TenantId tenantId, UserId userId, long createdTimeOffset, AlarmId idOffset, int limit);

    List<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, long createdTimeOffset, AlarmId idOffset, int limit);

    int deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId);

    void deleteEntityAlarmRecordsByTenantId(TenantId tenantId);

    long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmCountQuery query);

    long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmCountQuery query, Collection<EntityId> orderedEntityIds);

    PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink);

    List<UUID> findActiveOriginatorAlarms(TenantId tenantId, OriginatorAlarmFilter originatorAlarmFilter, int limit);

}
