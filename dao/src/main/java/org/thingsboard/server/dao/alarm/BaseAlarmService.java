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
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AlarmDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseAlarmService extends AbstractEntityService implements AlarmService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    private final EntityService entityService;
    private final DataValidator<Alarm> alarmDataValidator;

    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        validateAlarmRequest(request);
        return withPropagated(alarmDao.updateAlarm(request));
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
        return withPropagated(result);
    }

    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs) {
        return withPropagated(alarmDao.acknowledgeAlarm(tenantId, alarmId, ackTs));
    }

    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details) {
        return withPropagated(alarmDao.clearAlarm(tenantId, alarmId, clearTs, details));
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(Alarm alarm) {
        return createOrUpdateAlarm(alarm, true);
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(Alarm alarm, boolean alarmCreationEnabled) {
        alarmDataValidator.validate(alarm, Alarm::getTenantId);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            alarm.setCustomerId(entityService.fetchEntityCustomerId(alarm.getTenantId(), alarm.getOriginator()).orElse(null));
            if (alarm.getId() == null) {
                // Atomic update and return alarm + assignee.
                Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType());
                if (existing == null || existing.getStatus().isCleared()) {
                    if (!alarmCreationEnabled) {
                        throw new ApiUsageLimitsExceededException("Alarms creation is disabled");
                    }
                    return createAlarm(alarm);
                } else {
                    return updateAlarm(existing, alarm);
                }
            } else {
                return updateAlarm(alarm);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestByOriginatorAndTypeAsync(tenantId, originator, type);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityDataPageLink(query.getPageLink());
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    @Override
    @Transactional
    public AlarmApiCallResult delAlarm(TenantId tenantId, AlarmId alarmId) {
        log.debug("Deleting Alarm Id: {}", alarmId);
        AlarmInfo alarm = alarmDao.findAlarmInfoById(tenantId, alarmId.getId());
        if (alarm == null) {
            return AlarmApiCallResult.builder().successful(false).build();
        } else {
            deleteEntityRelations(tenantId, alarm.getId());
            alarmDao.removeById(tenantId, alarm.getUuidId());
            return AlarmApiCallResult.builder().alarm(alarm).deleted(true).successful(true).build();
        }
    }

    @Override
    @Transactional
    public AlarmOperationResult deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        log.debug("Deleting Alarm Id: {}", alarmId);
        Alarm alarm = alarmDao.findAlarmById(tenantId, alarmId.getId());
        if (alarm == null) {
            return new AlarmOperationResult(alarm, false);
        }
        AlarmOperationResult result = new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
        deleteEntityRelations(tenantId, alarm.getId());
        alarmDao.removeById(tenantId, alarm.getUuidId());
        return result;
    }

    private AlarmOperationResult createAlarm(Alarm alarm) throws InterruptedException, ExecutionException {
        log.debug("New Alarm : {}", alarm);
        Alarm saved = alarmDao.save(alarm.getTenantId(), alarm);
        List<EntityId> propagatedEntitiesList = createEntityAlarmRecords(saved);
        return new AlarmOperationResult(saved, true, true, propagatedEntitiesList);
    }

    private List<EntityId> createEntityAlarmRecords(Alarm alarm) throws ExecutionException, InterruptedException {
        Set<EntityId> propagatedEntitiesSet = new LinkedHashSet<>();
        propagatedEntitiesSet.add(alarm.getOriginator());
        if (alarm.isPropagate()) {
            propagatedEntitiesSet.addAll(getRelatedEntities(alarm));
        }
        if (alarm.isPropagateToOwner()) {
            propagatedEntitiesSet.add(alarm.getCustomerId() != null ? alarm.getCustomerId() : alarm.getTenantId());
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
        EntityRelationsQuery query = new EntityRelationsQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, false);
        query.setParameters(parameters);
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> relations = relationService.findByQuery(alarm.getTenantId(), query).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            relations = relations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        return relations.map(EntityRelation::getFrom).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private AlarmOperationResult updateAlarm(Alarm update) {
        alarmDataValidator.validate(update, Alarm::getTenantId);
        return getAndUpdate(update.getTenantId(), update.getId(),
                (alarm) -> alarm == null ? null : updateAlarm(alarm, update));
    }

    private AlarmOperationResult updateAlarm(Alarm oldAlarm, Alarm newAlarm) {
        boolean propagationEnabled = !oldAlarm.isPropagate() && newAlarm.isPropagate();
        boolean propagationToOwnerEnabled = !oldAlarm.isPropagateToOwner() && newAlarm.isPropagateToOwner();
        boolean propagationToTenantEnabled = !oldAlarm.isPropagateToTenant() && newAlarm.isPropagateToTenant();
        AlarmSeverity oldAlarmSeverity = oldAlarm.getSeverity();
        Alarm result = alarmDao.save(newAlarm.getTenantId(), merge(oldAlarm, newAlarm));
        List<EntityId> propagatedEntitiesList;
        if (propagationEnabled || propagationToOwnerEnabled || propagationToTenantEnabled) {
            try {
                propagatedEntitiesList = createEntityAlarmRecords(result);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update alarm relations [{}]", result, e);
                throw new RuntimeException(e);
            }
        } else {
            propagatedEntitiesList = new ArrayList<>(getPropagationEntityIds(result));
        }
        return new AlarmOperationResult(result, true, false, oldAlarmSeverity, propagatedEntitiesList);
    }

    @Override
    public ListenableFuture<AlarmOperationResult> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTime) {
        Alarm alarm = alarmDao.findAlarmById(tenantId, alarmId.getId());
        if (alarm == null || alarm.getStatus().isAck()) {
            return Futures.immediateFuture(new AlarmOperationResult(alarm, false));
        } else {
            alarm.setAcknowledged(true);
            alarm.setAckTs(ackTime);
            alarm = alarmDao.save(alarm.getTenantId(), alarm);
            return Futures.immediateFuture(new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm))));
        }
    }

    @Override
    public ListenableFuture<AlarmOperationResult> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTime) {
        Alarm alarm = alarmDao.findAlarmById(tenantId, alarmId.getId());
        if (alarm == null || alarm.getStatus().isCleared()) {
            return Futures.immediateFuture(new AlarmOperationResult(alarm, false));
        } else {
            alarm.setCleared(true);
            alarm.setClearTs(clearTime);
            if (details != null) {
                alarm.setDetails(details);
            }
            alarm = alarmDao.save(alarm.getTenantId(), alarm);
            return Futures.immediateFuture(new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm))));
        }
    }

    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTime) {
        return withPropagated(alarmDao.assignAlarm(tenantId, alarmId, assigneeId, assignTime));
    }

    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long unassignTime) {
        return withPropagated(alarmDao.unassignAlarm(tenantId, alarmId, unassignTime));
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmById(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmInfoById(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        return Futures.immediateFuture(alarmDao.findAlarms(tenantId, query));
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return Futures.immediateFuture(alarmDao.findCustomerAlarms(tenantId, customerId, query));
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        return Futures.immediateFuture(alarmDao.findAlarmsV2(tenantId, query));
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        return Futures.immediateFuture(alarmDao.findCustomerAlarmsV2(tenantId, customerId, query));
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
    public void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId) {
        alarmDao.deleteEntityAlarmRecords(tenantId, entityId);
    }

    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, AlarmCountQuery query) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return alarmDao.countAlarmsByQuery(tenantId, customerId, query);
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

    private Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        if (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToTenant()) {
            List<EntityAlarm> entityAlarms = alarmDao.findEntityAlarmRecords(alarm.getTenantId(), alarm.getId());
            return entityAlarms.stream().map(EntityAlarm::getEntityId).collect(Collectors.toSet());
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    private void createEntityAlarmRecord(TenantId tenantId, EntityId entityId, Alarm alarm) {
        EntityAlarm entityAlarm = new EntityAlarm(tenantId, entityId, alarm.getCreatedTime(), alarm.getType(), alarm.getCustomerId(), null, alarm.getId());
        try {
            alarmDao.createEntityAlarmRecord(entityAlarm);
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity alarm record: {}", tenantId, entityAlarm, e);
        }
    }

    private <T> T getAndUpdate(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        Alarm entity = alarmDao.findAlarmById(tenantId, alarmId.getId());
        return function.apply(entity);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAlarmById(tenantId, new AlarmId(entityId.getId())));
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
