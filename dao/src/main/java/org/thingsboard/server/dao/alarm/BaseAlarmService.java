/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmService extends AbstractEntityService implements AlarmService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private EntityService entityService;

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("alarm-service"));
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    @Override
    public AlarmOperationResult createOrUpdateAlarm(Alarm alarm) {
        alarmDataValidator.validate(alarm, Alarm::getTenantId);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            if (alarm.getId() == null) {
                Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType()).get();
                if (existing == null || existing.getStatus().isCleared()) {
                    return createAlarm(alarm);
                } else {
                    return updateAlarm(existing, alarm);
                }
            } else {
                return updateAlarm(alarm).get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestByOriginatorAndType(tenantId, originator, type);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, CustomerId customerId,
                                                               AlarmDataPageLink pageLink, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, customerId, pageLink, orderedEntityIds);
    }

    @Override
    public AlarmOperationResult deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        try {
            log.debug("Deleting Alarm Id: {}", alarmId);
            Alarm alarm = alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId()).get();
            if (alarm == null) {
                return new AlarmOperationResult(alarm, false);
            }
            AlarmOperationResult result = new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
            deleteEntityRelations(tenantId, alarm.getId());
            alarmDao.deleteAlarm(tenantId, alarm);
            return result;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private AlarmOperationResult createAlarm(Alarm alarm) throws InterruptedException, ExecutionException {
        log.debug("New Alarm : {}", alarm);
        Alarm saved = alarmDao.save(alarm.getTenantId(), alarm);
        List<EntityId> propagatedEntitiesList = createAlarmRelations(saved);
        return new AlarmOperationResult(saved, true, propagatedEntitiesList);
    }

    private List<EntityId> createAlarmRelations(Alarm alarm) throws InterruptedException, ExecutionException {
        List<EntityId> propagatedEntitiesList;
        if (alarm.isPropagate()) {
            Set<EntityId> parentEntities = getParentEntities(alarm);
            propagatedEntitiesList = new ArrayList<>(parentEntities.size() + 1);
            for (EntityId parentId : parentEntities) {
                propagatedEntitiesList.add(parentId);
                createAlarmRelation(alarm.getTenantId(), parentId, alarm.getId());
            }
            propagatedEntitiesList.add(alarm.getOriginator());
        } else {
            propagatedEntitiesList = Collections.singletonList(alarm.getOriginator());
        }
        return propagatedEntitiesList;
    }

    private Set<EntityId> getParentEntities(Alarm alarm) throws InterruptedException, ExecutionException {
        EntityRelationsQuery query = new EntityRelationsQuery();
        //TODO 3.1: @dlandiak we need to fetch max 3 levels and then fetch more if needed and there is at least one non-duplicate.
        RelationsSearchParameters parameters = new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, false);
        query.setParameters(parameters);
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> relations = relationService.findByQuery(alarm.getTenantId(), query).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            relations = relations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        return relations.map(EntityRelation::getFrom).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ListenableFuture<AlarmOperationResult> updateAlarm(Alarm update) {
        alarmDataValidator.validate(update, Alarm::getTenantId);
        return getAndUpdate(update.getTenantId(), update.getId(), new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null) {
                    return null;
                } else {
                    return updateAlarm(alarm, update);
                }
            }
        });
    }

    private AlarmOperationResult updateAlarm(Alarm oldAlarm, Alarm newAlarm) {
        boolean oldPropagate = oldAlarm.isPropagate();
        boolean newPropagate = newAlarm.isPropagate();
        Alarm result = alarmDao.save(newAlarm.getTenantId(), merge(oldAlarm, newAlarm));
        List<EntityId> propagatedEntitiesList;
        if (!oldPropagate && newPropagate) {
            try {
                propagatedEntitiesList = createAlarmRelations(result);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update alarm relations [{}]", result, e);
                throw new RuntimeException(e);
            }
        } else {
            propagatedEntitiesList = new ArrayList<>(getPropagationEntityIds(result));
        }
        return new AlarmOperationResult(result, true, propagatedEntitiesList);
    }

    @Override
    public ListenableFuture<AlarmOperationResult> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTime) {
        return getAndUpdate(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isAck()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK;
                    alarm.setStatus(newStatus);
                    alarm.setAckTs(ackTime);
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @Override
    public ListenableFuture<AlarmOperationResult> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTime) {
        return getAndUpdate(tenantId, alarmId, new Function<Alarm, AlarmOperationResult>() {
            @Nullable
            @Override
            public AlarmOperationResult apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isCleared()) {
                    return new AlarmOperationResult(alarm, false);
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
                    alarm.setStatus(newStatus);
                    alarm.setClearTs(clearTime);
                    if (details != null) {
                        alarm.setDetails(details);
                    }
                    alarm = alarmDao.save(alarm.getTenantId(), alarm);
                    return new AlarmOperationResult(alarm, true, new ArrayList<>(getPropagationEntityIds(alarm)));
                }
            }
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return Futures.transformAsync(alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId()),
                a -> {
                    AlarmInfo alarmInfo = new AlarmInfo(a);
                    return Futures.transform(
                            entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                                alarmInfo.setOriginatorName(originatorName);
                                return alarmInfo;
                            }, MoreExecutors.directExecutor());
                }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        PageData<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(alarms.getData().size());
            for (AlarmInfo alarmInfo : alarms.getData()) {
                alarmFutures.add(Futures.transform(
                        entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                            if (originatorName == null) {
                                originatorName = "Deleted";
                            }
                            alarmInfo.setOriginatorName(originatorName);
                            return alarmInfo;
                        }, MoreExecutors.directExecutor()
                ));
            }
            return Futures.transform(Futures.successfulAsList(alarmFutures),
                    alarmInfos -> new PageData(alarmInfos, alarms.getTotalPages(), alarms.getTotalElements(),
                            alarms.hasNext()), MoreExecutors.directExecutor());
        }
        return Futures.immediateFuture(alarms);
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                                  AlarmStatus alarmStatus) {
        TimePageLink nextPageLink = new TimePageLink(100);
        boolean hasNext = true;
        AlarmSeverity highestSeverity = null;
        AlarmQuery query;
        while (hasNext && AlarmSeverity.CRITICAL != highestSeverity) {
            query = new AlarmQuery(entityId, nextPageLink, alarmSearchStatus, alarmStatus, false, null);
            PageData<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, query);
            if (alarms.hasNext()) {
                nextPageLink = nextPageLink.nextPageLink();
            }
            AlarmSeverity severity = detectHighestSeverity(alarms.getData());
            if (severity == null) {
                continue;
            }
            if (severity == AlarmSeverity.CRITICAL || highestSeverity == null) {
                highestSeverity = severity;
            } else {
                highestSeverity = highestSeverity.compareTo(severity) < 0 ? highestSeverity : severity;
            }
        }
        return highestSeverity;
    }

    private AlarmSeverity detectHighestSeverity(List<AlarmInfo> alarms) {
        if (!alarms.isEmpty()) {
            List<AlarmInfo> sorted = new ArrayList(alarms);
            sorted.sort(Comparator.comparing(Alarm::getSeverity));
            return sorted.get(0).getSeverity();
        } else {
            return null;
        }
    }

    private void deleteRelation(TenantId tenantId, EntityRelation alarmRelation) {
        log.debug("Deleting Alarm relation: {}", alarmRelation);
        relationService.deleteRelation(tenantId, alarmRelation);
    }

    private void createRelation(TenantId tenantId, EntityRelation alarmRelation) {
        log.debug("Creating Alarm relation: {}", alarmRelation);
        relationService.saveRelation(tenantId, alarmRelation);
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
        existing.setStatus(alarm.getStatus());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
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

    private Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        List<EntityRelation> relations = relationService.findByTo(alarm.getTenantId(), alarm.getId(), RelationTypeGroup.ALARM);
        return relations.stream().map(EntityRelation::getFrom).collect(Collectors.toSet());
    }

    private void createAlarmRelation(TenantId tenantId, EntityId entityId, EntityId alarmId) {
        createRelation(tenantId, new EntityRelation(entityId, alarmId, AlarmSearchStatus.ANY.name(), RelationTypeGroup.ALARM));
    }

    private <T> ListenableFuture<T> getAndUpdate(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        ListenableFuture<Alarm> entity = alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
        return Futures.transform(entity, function, readResultsProcessingExecutor);
    }

    private DataValidator<Alarm> alarmDataValidator =
            new DataValidator<Alarm>() {

                @Override
                protected void validateDataImpl(TenantId tenantId, Alarm alarm) {
                    if (StringUtils.isEmpty(alarm.getType())) {
                        throw new DataValidationException("Alarm type should be specified!");
                    }
                    if (alarm.getOriginator() == null) {
                        throw new DataValidationException("Alarm originator should be specified!");
                    }
                    if (alarm.getSeverity() == null) {
                        throw new DataValidationException("Alarm severity should be specified!");
                    }
                    if (alarm.getStatus() == null) {
                        throw new DataValidationException("Alarm status should be specified!");
                    }
                    if (alarm.getTenantId() == null) {
                        throw new DataValidationException("Alarm should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(alarm.getTenantId(), alarm.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Alarm is referencing to non-existent tenant!");
                        }
                    }
                }
            };
}
