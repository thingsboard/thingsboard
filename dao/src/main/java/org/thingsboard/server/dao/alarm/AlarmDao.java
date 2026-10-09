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
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
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
import org.thingsboard.server.dao.Dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AlarmDao extends Dao<Alarm> {

    Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type);

    ListenableFuture<Alarm> findLatestByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type);

    Alarm findAlarmById(TenantId tenantId, UUID key);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key);

    AlarmInfo findAlarmInfoById(TenantId tenantId, UUID key);

    AlarmInfo findAlarmInfoByOriginatorAndId(TenantId tenantId, EntityId originator, AlarmId alarmId);

    Alarm save(TenantId tenantId, Alarm alarm);

    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query);

    PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query);

    long findAlarmCount(TenantId tenantId, AlarmQuery query, AlarmFilter filter);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions,
                                                        AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, AlarmStatusFilter asf, String assigneeId);

    /**
     * The returned {@link PageData} carries no totals: the count query is deliberately skipped (it would be
     * a cross-shard count(*) on every TTL batch), so totalPages/totalElements are always 0. Callers may only
     * rely on {@link PageData#getData()} and {@link PageData#hasNext()}.
     */
    PageData<AlarmRef> findExpiredAlarmRefsByTenantId(Long time, TenantId tenantId, PageLink pageLink);

    PageData<AlarmRef> findAlarmRefsByAssigneeId(TenantId tenantId, UserId userId, long createdTimeOffset, AlarmId idOffset, int limit);

    int unassignAlarmsByAssignee(TenantId tenantId, UserId assigneeId, long unassignTs);

    PageData<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, long createdTimeOffset, AlarmId idOffset, int limit);

    void createEntityAlarmRecord(EntityAlarm entityAlarm);

    List<EntityAlarm> findEntityAlarmRecords(TenantId tenantId, EntityId originator, AlarmId id);

    List<EntityAlarm> findEntityAlarmRecordsByEntityTypes(TenantId tenantId, EntityId originator, AlarmId id, List<EntityType> types);

    List<EntityAlarm> findEntityAlarmRecordsByEntityId(TenantId tenantId, EntityId entityId);

    int deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId);

    void deleteEntityAlarmRecordsByTenantId(TenantId tenantId);

    AlarmApiCallResult createOrUpdateActiveAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled);

    AlarmApiCallResult updateAlarm(AlarmUpdateRequest request);

    AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, EntityId originator, AlarmId id, long ackTs);

    AlarmApiCallResult clearAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long clearTs, JsonNode details);

    AlarmApiCallResult assignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, UserId assigneeId, long assignTime);

    AlarmApiCallResult unassignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long unassignTime);

    void removeByOriginatorAndId(TenantId tenantId, EntityId originator, AlarmId alarmId);

    long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmCountQuery query, Collection<EntityId> orderedEntityIds);

    PageData<EntitySubtype> findTenantAlarmTypes(UUID tenantId, PageLink pageLink);

    List<String> findTenantAlarmTypeNames(UUID tenantId, int limit);

    boolean addAlarmType(UUID tenantId, String type);

    boolean removeAlarmTypesIfNoAlarmsPresent(UUID tenantId, Set<String> types);

    List<UUID> findActiveOriginatorAlarms(TenantId tenantId, OriginatorAlarmFilter originatorAlarmFilter, int limit);

}
