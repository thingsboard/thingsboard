/**
 * Copyright © 2016-2017 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.alarm;


import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.entity.BaseEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.*;
import org.thingsboard.server.dao.relation.EntityRelationsQuery;
import org.thingsboard.server.dao.relation.EntitySearchDirection;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.*;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseAlarmService extends BaseEntityService implements AlarmService {

    public static final String ALARM_RELATION_PREFIX = "ALARM_";
    public static final String ALARM_RELATION = "ALARM_ANY";

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private RelationService relationService;

    @Override
    public Alarm createOrUpdateAlarm(Alarm alarm) {
        alarmDataValidator.validate(alarm);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType()).get();
            if (existing == null || existing.getStatus().isCleared()) {
                log.debug("New Alarm : {}", alarm);
                Alarm saved = getData(alarmDao.save(new AlarmEntity(alarm)));
                EntityRelationsQuery query = new EntityRelationsQuery();
                query.setParameters(new RelationsSearchParameters(saved.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE));
                List<EntityId> parentEntities = relationService.findByQuery(query).get().stream().map(r -> r.getFrom()).collect(Collectors.toList());
                for (EntityId parentId : parentEntities) {
                    createRelation(new EntityRelation(parentId, saved.getId(), ALARM_RELATION));
                    createRelation(new EntityRelation(parentId, saved.getId(), ALARM_RELATION_PREFIX + saved.getStatus().name()));
                }
                return saved;
            } else {
                log.debug("Alarm before merge: {}", alarm);
                alarm = merge(existing, alarm);
                log.debug("Alarm after merge: {}", alarm);
                return getData(alarmDao.save(new AlarmEntity(alarm)));
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<Boolean> updateAlarm(Alarm update) {
        alarmDataValidator.validate(update);
        return getAndUpdate(update.getId(), new Function<Alarm, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Alarm alarm) {
                if (alarm == null) {
                    return false;
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = update.getStatus();
                    alarmDao.save(new AlarmEntity(merge(alarm, update)));
                    if (oldStatus != newStatus) {
                        updateRelations(alarm, oldStatus, newStatus);
                    }
                    return true;
                }
            }
        });
    }

    @Override
    public ListenableFuture<Boolean> ackAlarm(AlarmId alarmId, long ackTime) {
        return getAndUpdate(alarmId, new Function<Alarm, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isAck()) {
                    return false;
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK;
                    alarm.setStatus(newStatus);
                    alarm.setAckTs(ackTime);
                    alarmDao.save(new AlarmEntity(alarm));
                    updateRelations(alarm, oldStatus, newStatus);
                    return true;
                }
            }
        });
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(AlarmId alarmId, long clearTime) {
        return getAndUpdate(alarmId, new Function<Alarm, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isCleared()) {
                    return false;
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
                    alarm.setStatus(newStatus);
                    alarm.setClearTs(clearTime);
                    alarmDao.save(new AlarmEntity(alarm));
                    updateRelations(alarm, oldStatus, newStatus);
                    return true;
                }
            }
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmById(AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(alarmId.getId());
    }

    @Override
    public ListenableFuture<TimePageData<Alarm>> findAlarms(AlarmQuery query) {
        ListenableFuture<List<Alarm>> alarms = alarmDao.findAlarms(query);
        return Futures.transform(alarms, new Function<List<Alarm>, TimePageData<Alarm>>() {
            @Nullable
            @Override
            public TimePageData<Alarm> apply(@Nullable List<Alarm> alarms) {
                return new TimePageData<>(alarms, query.getPageLink());
            }
        });
    }

    private void deleteRelation(EntityRelation alarmRelation) throws ExecutionException, InterruptedException {
        log.debug("Deleting Alarm relation: {}", alarmRelation);
        relationService.deleteRelation(alarmRelation).get();
    }

    private void createRelation(EntityRelation alarmRelation) throws ExecutionException, InterruptedException {
        log.debug("Creating Alarm relation: {}", alarmRelation);
        relationService.saveRelation(alarmRelation).get();
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
        return existing;
    }

    private void updateRelations(Alarm alarm, AlarmStatus oldStatus, AlarmStatus newStatus) {
        try {
            EntityRelationsQuery query = new EntityRelationsQuery();
            query.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE));
            List<EntityId> parentEntities = relationService.findByQuery(query).get().stream().map(r -> r.getFrom()).collect(Collectors.toList());
            for (EntityId parentId : parentEntities) {
                deleteRelation(new EntityRelation(parentId, alarm.getId(), ALARM_RELATION_PREFIX + oldStatus.name()));
                createRelation(new EntityRelation(parentId, alarm.getId(), ALARM_RELATION_PREFIX + newStatus.name()));
            }
        } catch (ExecutionException | InterruptedException e) {
            log.warn("[{}] Failed to update relations. Old status: [{}], New status: [{}]", alarm.getId(), oldStatus, newStatus);
            throw new RuntimeException(e);
        }
    }

    private ListenableFuture<Boolean> getAndUpdate(AlarmId alarmId, Function<Alarm, Boolean> function) {
        validateId(alarmId, "Alarm id should be specified!");
        ListenableFuture<Alarm> entity = alarmDao.findAlarmByIdAsync(alarmId.getId());
        return Futures.transform(entity, function);
    }

    private DataValidator<Alarm> alarmDataValidator =
            new DataValidator<Alarm>() {

                @Override
                protected void validateDataImpl(Alarm alarm) {
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
                        TenantEntity tenant = tenantDao.findById(alarm.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Alarm is referencing to non-existent tenant!");
                        }
                    }
                }
            };
}
