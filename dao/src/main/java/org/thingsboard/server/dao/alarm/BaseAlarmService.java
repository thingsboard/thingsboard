// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.OriginatorAlarmFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.ActionCause;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AlarmDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseAlarmService extends AbstractCachedEntityService<TenantId, PageData<EntitySubtype>, AlarmTypesCacheEvictEvent> implements AlarmService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private static final PageLink DEFAULT_ALARM_TYPES_PAGE_LINK = new PageLink(25, 0, null, new SortOrder("type"));
    // Upper bound on the per-tenant set of type names kept in the registration cache. A tenant with more distinct
    // types than this simply falls through to the idempotent insert on registration -- correct, just not cache-skipped.
    // Configurable so a tenant with thousands of distinct alarm types can raise the cap instead of paying the insert
    // (a 2PC reference-table write across all nodes under Citus) for every type past the default.
    @Value("${cache.alarms.max_alarm_type_names_per_tenant:1000}")
    private int maxCachedAlarmTypeNames;

    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    private final EntityService entityService;
    private final OwnerService ownerService;

    // Dedicated per-tenant registration cache: the set of known alarm type names, used only to skip the reference-table
    // insert for an already-registered type. Deliberately lightweight (plain names, not EntitySubtype pages) and kept
    // separate from the UI-facing alarmTypes page cache.
    @Autowired
    @Qualifier("AlarmTypeNamesCache")
    private TbTransactionalCache<TenantId, HashSet<String>> alarmTypeNamesCache;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AlarmTypesCacheEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
        alarmTypeNamesCache.evict(tenantId);
    }

    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        validateAlarmRequest(request);
        AlarmApiCallResult result = withPropagated(alarmDao.updateAlarm(request));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getAlarm().getTenantId()).entity(result)
                    .entityId(result.getAlarm().getId()).build());
        }
        return result;
    }

    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request) {
        return createAlarm(request, true);
    }

    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled) {
        validateAlarmRequest(request);
        CustomerId customerId = entityService.fetchEntityCustomerId(request.getTenantId(), request.getOriginator()).orElse(null);
        if (customerId == null && request.getCustomerId() != null) {
            throw new DataValidationException("Can't assign alarm to customer. Originator is not assigned to customer!");
        } else if (customerId != null && request.getCustomerId() != null && !customerId.equals(request.getCustomerId())) {
            throw new DataValidationException("Can't assign alarm to customer. Originator belongs to different customer!");
        }
        request.setCustomerId(customerId);
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, alarmCreationEnabled);
        if (!result.isSuccessful() && !alarmCreationEnabled) {
            throw new ApiUsageLimitsExceededException("Alarms creation is disabled");
        }
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getAlarm().getTenantId())
                    .entityId(result.getAlarm().getId()).entity(result).created(true).build());
            try {
                // Best-effort (nothing FK-references alarm_types): the alarm row is already committed, so a failure
                // here (cache outage, unreachable Citus worker blocking the reference-table 2PC) must not abort the
                // entity_alarm record creation in withPropagated() below -- a retry would take the update branch and
                // never re-create those records, leaving the alarm invisible to affected-entity queries.
                registerAlarmType(request.getTenantId(), request.getType());
            } catch (Exception e) {
                log.error("[{}] Failed to register alarm type [{}]", request.getTenantId(), request.getType(), e);
            }
        }
        return withPropagated(result);
    }

    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long ackTs) {
        if (originator == null) {
            return failOnMissingOriginator(alarmId);
        }
        var result = withPropagated(alarmDao.acknowledgeAlarm(tenantId, originator, alarmId, ackTs));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(result.getAlarm().getId())
                    .entity(result.getAlarm())
                    .actionType(ActionType.ALARM_ACK)
                    .build());
        }
        return result;
    }

    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long clearTs, JsonNode details, boolean pushEvent) {
        if (originator == null) {
            return failOnMissingOriginator(alarmId);
        }
        var result = withPropagated(alarmDao.clearAlarm(tenantId, originator, alarmId, clearTs, details));
        if (pushEvent && result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(result.getAlarm().getId())
                    .entity(result.getAlarm())
                    .actionType(ActionType.ALARM_CLEAR)
                    .build());
        }
        return result;
    }

    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestActiveByOriginatorAndTypeAsync(tenantId, originator, type);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, MergedUserPermissions mergedUserPermissions,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateEntityDataPageLink(query.getPageLink());
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, query, orderedEntityIds);
    }

    @Override
    @Transactional
    public AlarmApiCallResult delAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId) {
        return delAlarm(tenantId, originator, alarmId, true);
    }

    @Override
    @Transactional
    public AlarmApiCallResult delAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, boolean checkAndDeleteAlarmType) {
        if (originator == null) {
            // The originator is the distribution column of the sharded alarm table; a null would misroute the
            // single-shard lookup below. Guard as acknowledge/clear/assign/unassign do (delAlarm is equally exposed
            // to custom rule nodes via RuleEngineAlarmService.deleteAlarm).
            return failOnMissingOriginator(alarmId);
        }
        // Single-shard load by (originator_id, id): the originator routes the lookup to the one shard owning the alarm,
        // avoiding the cross-shard fan-out of a by-id-only load on the originator-sharded alarm table.
        AlarmInfo alarm = alarmDao.findAlarmInfoByOriginatorAndId(tenantId, originator, alarmId);
        return deleteAlarm(tenantId, alarm, checkAndDeleteAlarmType);
    }

    /**
     * Clears the assignee of every alarm assigned to the given user in a single multi-shard statement (see
     * {@link AlarmDao#unassignAlarmsByAssignee}). This is the Citus deleted-user-cleanup path: it deliberately omits
     * the per-alarm side effects that the plain-Postgres path in {@code AlarmsUnassignTaskProcessor} still emits,
     * because per-alarm unassign on the originator-sharded alarm table would be a cross-shard lookup each:
     * <ul>
     * <li>the {@code UNASSIGNED_FROM_DELETED_USER} system comment and the {@code ALARM_UNASSIGNED} audit/notification.
     * Dropping those is safe for EDQS: alarm assignment is not event-sourced there (only {@code ActionEntityEvent} is
     * published, which the EDQS listener ignores), so no index drift is introduced;</li>
     * <li>the {@code ALARM_UNASSIGNED} rule-engine message, so a push-to-edge rule node produces no edge events: edge
     * copies of these alarms keep the deleted user as assignee until the alarm is next touched;</li>
     * <li>websocket subscription updates: open alarm widgets keep showing the deleted user until the next refresh.</li>
     * </ul>
     * All of the above are accepted trade-offs for this bounded deleted-user cleanup.
     */
    @Override
    @Transactional
    public int unassignAlarmsByAssignee(TenantId tenantId, UserId assigneeId, long unassignTs) {
        return alarmDao.unassignAlarmsByAssignee(tenantId, assigneeId, unassignTs);
    }

    private AlarmApiCallResult deleteAlarm(TenantId tenantId, AlarmInfo alarm, boolean deleteAlarmTypes) {
        if (alarm == null) {
            return AlarmApiCallResult.builder().successful(false).build();
        } else {
            log.debug("[{}][{}] Executing deleteAlarm [{}]", tenantId, alarm.getOriginator(), alarm.getId());
            var propagationIds = getPropagationEntityIdsList(alarm);
            alarmDao.removeByOriginatorAndId(tenantId, alarm.getOriginator(), alarm.getId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(alarm.getId())
                    .entity(alarm)
                    // Bulk callers (deleteAlarmTypes == false) batch the alarm-type and alarm-comment cleanup
                    // themselves, so flag the event to keep the per-alarm deletion listener from also enqueuing them.
                    .cause(deleteAlarmTypes ? null : ActionCause.BULK_DELETION)
                    .build());
            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(alarm.getId())
                    .entity(alarm)
                    .actionType(ActionType.ALARM_DELETE)
                    .build());
            if (deleteAlarmTypes) {
                delAlarmTypes(tenantId, Collections.singleton(alarm.getType()));
            }
            return AlarmApiCallResult.builder().alarm(alarm).deleted(true).successful(true).propagatedEntitiesList(propagationIds).build();
        }
    }

    @Override
    @Transactional
    public void delAlarmTypes(TenantId tenantId, Set<String> types) {
        if (!types.isEmpty() && alarmDao.removeAlarmTypesIfNoAlarmsPresent(tenantId.getId(), types)) {
            publishEvictEvent(new AlarmTypesCacheEvictEvent(tenantId));
        }
    }

    private List<EntityId> createEntityAlarmRecords(Alarm alarm) throws ExecutionException, InterruptedException {
        Set<EntityId> propagatedEntitiesSet = new LinkedHashSet<>();
        propagatedEntitiesSet.add(alarm.getOriginator());
        if (alarm.isPropagate()) {
            propagatedEntitiesSet.addAll(getRelatedEntities(alarm));
        }
        if (alarm.isPropagateToOwnerHierarchy()) {
            propagatedEntitiesSet.addAll(ownerService.getOwners(alarm.getTenantId(), alarm.getOriginator()));
        } else if (alarm.isPropagateToOwner()) {
            EntityId owner = ownerService.getOwner(alarm.getTenantId(), alarm.getOriginator());
            if (owner != null) {
                propagatedEntitiesSet.add(owner);
            }
        }
        if (alarm.isPropagateToTenant()) {
            propagatedEntitiesSet.add(alarm.getTenantId());
        }
        for (EntityId entityId : propagatedEntitiesSet) {
            createEntityAlarmRecord(alarm.getTenantId(), entityId, alarm);
        }
        return new ArrayList<>(propagatedEntitiesSet);
    }

    private Set<EntityId> getRelatedEntities(Alarm alarm) throws InterruptedException, ExecutionException {
        EntityRelationsQuery commonQuery = new EntityRelationsQuery();
        commonQuery.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, RelationTypeGroup.COMMON, false));
        EntityRelationsQuery groupQuery = new EntityRelationsQuery();
        groupQuery.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, RelationTypeGroup.FROM_ENTITY_GROUP, false));
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> commonRelations = relationService.findByQuery(alarm.getTenantId(), commonQuery).get().stream();
        Stream<EntityRelation> groupRelations = relationService.findByQuery(alarm.getTenantId(), groupQuery).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            commonRelations = commonRelations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        Set<EntityId> parentEntities = new LinkedHashSet<>();
        parentEntities.addAll(commonRelations.map(EntityRelation::getFrom).toList());
        parentEntities.addAll(groupRelations.map(EntityRelation::getFrom).toList());
        return parentEntities;
    }

    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, UserId assigneeId, long assignTime) {
        if (originator == null) {
            return failOnMissingOriginator(alarmId);
        }
        var result = withPropagated(alarmDao.assignAlarm(tenantId, originator, alarmId, assigneeId, assignTime));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_ASSIGNED).build());
        }
        return result;
    }

    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, EntityId originator, AlarmId alarmId, long unassignTime) {
        if (originator == null) {
            return failOnMissingOriginator(alarmId);
        }
        var result = withPropagated(alarmDao.unassignAlarm(tenantId, originator, alarmId, unassignTime));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_UNASSIGNED).build());
        }
        return result;
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, id -> "Incorrect alarmId " + id);
        return alarmDao.findAlarmById(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmByIdAsync [{}]", alarmId);
        validateId(alarmId, id -> "Incorrect alarmId " + id);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, id -> "Incorrect alarmId " + id);
        return alarmDao.findAlarmInfoById(tenantId, alarmId.getId());
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        return alarmDao.findAlarms(tenantId, query);
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return alarmDao.findCustomerAlarms(tenantId, customerId, query);
    }

    @Override
    public PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        return alarmDao.findAlarmsV2(tenantId, query);
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        return alarmDao.findCustomerAlarmsV2(tenantId, customerId, query);
    }

    @Override
    public List<Long> findAlarmCounts(TenantId tenantId, AlarmQuery query, List<AlarmFilter> filters) {
        List<Long> alarmCounts = new ArrayList<>();
        for (AlarmFilter filter : filters) {
            long count = alarmDao.findAlarmCount(tenantId, query, filter);
            alarmCounts.add(count);
        }
        return alarmCounts;
    }

    @Override
    public List<AlarmRef> findAlarmRefsByAssigneeId(TenantId tenantId, UserId userId, long createdTimeOffset, AlarmId idOffset, int limit) {
        log.trace("[{}] Executing findAlarmRefsByAssigneeId [{}][{}][{}]", tenantId, userId, createdTimeOffset, idOffset);
        validateId(userId, id -> "Incorrect userId " + id);
        return alarmDao.findAlarmRefsByAssigneeId(tenantId, userId, createdTimeOffset, idOffset, limit).getData();
    }

    @Override
    public List<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, long createdTimeOffset, AlarmId idOffset, int limit) {
        log.trace("[{}] Executing findAlarmIdsByOriginatorIdAndIdOffset [{}][{}]", tenantId, originatorId, idOffset);
        return alarmDao.findAlarmIdsByOriginatorId(tenantId, originatorId, createdTimeOffset, idOffset, limit).getData();
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                                  AlarmStatus alarmStatus, String assigneeId) {
        AlarmStatusFilter asf;
        if (alarmSearchStatus != null) {
            asf = AlarmStatusFilter.from(alarmSearchStatus);
        } else if (alarmStatus != null) {
            asf = AlarmStatusFilter.from(alarmStatus);
        } else {
            asf = AlarmStatusFilter.empty();
        }

        Set<AlarmSeverity> alarmSeverities = alarmDao.findAlarmSeverities(tenantId, entityId, asf, assigneeId);
        return alarmSeverities.stream().min(AlarmSeverity::compareTo).orElse(null);
    }

    @Override
    public int deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityAlarms [{}]", entityId);
        return alarmDao.deleteEntityAlarmRecords(tenantId, entityId);
    }

    @Override
    public void deleteEntityAlarmRecordsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityAlarmRecordsByTenantId [{}]", tenantId);
        alarmDao.deleteEntityAlarmRecordsByTenantId(tenantId);
    }

    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmCountQuery query) {
        return countAlarmsByQuery(tenantId, customerId, mergedUserPermissions, query, null);
    }

    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions, AlarmCountQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return alarmDao.countAlarmsByQuery(tenantId, customerId, mergedUserPermissions, query, orderedEntityIds);
    }

    @Override
    public PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAlarmTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        if (DEFAULT_ALARM_TYPES_PAGE_LINK.equals(pageLink)) {
            return cache.getAndPutInTransaction(tenantId, () ->
                    alarmDao.findTenantAlarmTypes(tenantId.getId(), pageLink), false);
        }
        return alarmDao.findTenantAlarmTypes(tenantId.getId(), pageLink);
    }

    private void registerAlarmType(TenantId tenantId, String type) {
        // Fast path: if the type is in the cached set of known type names, it is already registered in the alarm_types
        // reference table and the insert can be skipped. A miss does NOT prove absence (the cached set is capped at
        // maxCachedAlarmTypeNames per tenant), so we fall through to the idempotent insert below.
        if (isKnownAlarmType(tenantId, type)) {
            return;
        }
        // The type was not found in the cache. Perform an idempotent insert into the reference table; the evict (so the
        // registration cache and findAlarmTypes reflect the new type immediately) fires ONLY when a row was actually
        // inserted, i.e. the (tenantId, type) pair was genuinely new. ON CONFLICT DO NOTHING guarantees race-safety
        // under concurrent alarm creation.
        if (alarmDao.addAlarmType(tenantId.getId(), type)) {
            publishEvictEvent(new AlarmTypesCacheEvictEvent(tenantId));
        }
    }

    private boolean isKnownAlarmType(TenantId tenantId, String type) {
        HashSet<String> knownTypes = alarmTypeNamesCache.getAndPutInTransaction(tenantId,
                () -> loadKnownAlarmTypeNames(tenantId), false);
        return knownTypes != null && knownTypes.contains(type);
    }

    private HashSet<String> loadKnownAlarmTypeNames(TenantId tenantId) {
        return new HashSet<>(alarmDao.findTenantAlarmTypeNames(tenantId.getId(), maxCachedAlarmTypeNames));
    }

    @Override
    public List<UUID> findActiveOriginatorAlarms(TenantId tenantId, OriginatorAlarmFilter originatorAlarmFilter, int limit) {
        log.trace("Executing findActiveOriginatorAlarms, tenantId [{}], originatorAlarmFilter [{}]", tenantId, originatorAlarmFilter);
        return alarmDao.findActiveOriginatorAlarms(tenantId, originatorAlarmFilter, limit);
    }

    private Alarm merge(Alarm existing, Alarm alarm) {
        if (alarm.getStartTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getStartTs());
        }
        if (alarm.getEndTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getEndTs());
        }
        if (alarm.getClearTs() > existing.getClearTs()) {
            existing.setClearTs(alarm.getClearTs());
        }
        if (alarm.getAckTs() > existing.getAckTs()) {
            existing.setAckTs(alarm.getAckTs());
        }
        if (alarm.getAssignTs() > existing.getAssignTs()) {
            existing.setAssignTs(alarm.getAssignTs());
        }
        existing.setAcknowledged(alarm.isAcknowledged());
        existing.setCleared(alarm.isCleared());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setCustomerId(alarm.getCustomerId());
        existing.setAssigneeId(alarm.getAssigneeId());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
        existing.setPropagateToOwner(existing.isPropagateToOwner() || alarm.isPropagateToOwner());
        existing.setPropagateToOwnerHierarchy(existing.isPropagateToOwnerHierarchy() || alarm.isPropagateToOwnerHierarchy());
        existing.setPropagateToTenant(existing.isPropagateToTenant() || alarm.isPropagateToTenant());
        List<String> existingPropagateRelationTypes = existing.getPropagateRelationTypes();
        List<String> newRelationTypes = alarm.getPropagateRelationTypes();
        if (!CollectionUtils.isEmpty(newRelationTypes)) {
            if (!CollectionUtils.isEmpty(existingPropagateRelationTypes)) {
                existing.setPropagateRelationTypes(Stream.concat(existingPropagateRelationTypes.stream(), newRelationTypes.stream())
                        .distinct()
                        .collect(Collectors.toList()));
            } else {
                existing.setPropagateRelationTypes(newRelationTypes);
            }
        }
        return existing;
    }

    private List<EntityId> getPropagationEntityIdsList(Alarm alarm) {
        return new ArrayList<>(getPropagationEntityIds(alarm));
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        return processGetPropagationEntityIds(alarm, null);
    }

    @Override
    public Set<EntityId> getPropagationEntityIds(Alarm alarm, List<EntityType> types) {
        return processGetPropagationEntityIds(alarm, types);
    }

    private Set<EntityId> processGetPropagationEntityIds(Alarm alarm, List<EntityType> types) {
        validateId(alarm.getId(), id -> "Alarm id should be specified!");
        validateEntityId(alarm.getOriginator(), id -> "Alarm originator should be specified!");
        if (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToTenant() || alarm.isPropagateToOwnerHierarchy()) {
            List<EntityAlarm> entityAlarms = CollectionUtils.isEmpty(types) ?
                    alarmDao.findEntityAlarmRecords(alarm.getTenantId(), alarm.getOriginator(), alarm.getId()) :
                    alarmDao.findEntityAlarmRecordsByEntityTypes(alarm.getTenantId(), alarm.getOriginator(), alarm.getId(), types);
            return entityAlarms.stream().map(EntityAlarm::getEntityId).collect(Collectors.toSet());
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private void createEntityAlarmRecord(TenantId tenantId, EntityId entityId, Alarm alarm) {
        EntityAlarm entityAlarm = new EntityAlarm(tenantId, entityId, alarm.getOriginator().getId(), alarm.getCreatedTime(), alarm.getType(), alarm.getCustomerId(), null, alarm.getId());
        try {
            alarmDao.createEntityAlarmRecord(entityAlarm);
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity alarm record: {}", tenantId, entityAlarm, e);
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAlarmById(tenantId, new AlarmId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

    //TODO: refactor to use efficient caching.
    private AlarmApiCallResult withPropagated(AlarmApiCallResult result) {
        if (result.isSuccessful() && result.getAlarm() != null) {
            List<EntityId> propagationEntities;
            if (result.isPropagationChanged()) {
                try {
                    propagationEntities = createEntityAlarmRecords(result.getAlarm());
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                propagationEntities = getPropagationEntityIdsList(result.getAlarm());
            }
            return new AlarmApiCallResult(result, propagationEntities);
        } else {
            return result;
        }
    }

    private AlarmApiCallResult failOnMissingOriginator(AlarmId alarmId) {
        // The originator routes the single-shard lookup, so it is required. A null one is a caller bug now; log it and
        // return an unsuccessful result (no exception), matching the pre-existing failure behavior of these operations.
        log.warn("[{}] Alarm operation skipped: originator must not be null", alarmId);
        return AlarmApiCallResult.builder().successful(false).build();
    }

    private void validateAlarmRequest(AlarmModificationRequest request) {
        ConstraintValidator.validateFields(request);
        if (request.getEndTs() > 0 && request.getStartTs() > request.getEndTs()) {
            throw new DataValidationException("Alarm start ts can't be greater then alarm end ts!");
        }
        if (!tenantService.tenantExists(request.getTenantId())) {
            throw new DataValidationException("Alarm is referencing to non-existent tenant!");
        }
        if (request.getStartTs() == 0L) {
            request.setStartTs(System.currentTimeMillis());
        }
        if (request.getEndTs() == 0L) {
            request.setEndTs(request.getStartTs());
        }
    }

}
