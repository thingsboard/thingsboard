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
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.query.AlarmQueryRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private AlarmQueryRepository alarmQueryRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected CrudRepository<AlarmEntity, UUID> getCrudRepository() {
        return alarmRepository;
    }

    @Override
    public Boolean deleteAlarm(TenantId tenantId, Alarm alarm) {
        return removeById(tenantId, alarm.getUuidId());
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> {
            List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                    originator.getId(),
                    type,
                    PageRequest.of(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        Set<AlarmStatus> statusSet = null;
        if (query.getSearchStatus() != null) {
            statusSet = query.getSearchStatus().getStatuses();
        } else if (query.getStatus() != null){
            statusSet = Collections.singleton(query.getStatus());
        }
        return DaoUtil.toPageData(
                alarmRepository.findAlarms(
                        tenantId.getId(),
                        affectedEntity.getId(),
                        affectedEntity.getEntityType().name(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        new ArrayList<>(statusSet),
                        Objects.toString(query.getPageLink().getTextSearch(), ""),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, CustomerId customerId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.findAlarmDataByQueryForEntities(tenantId, customerId, query, orderedEntityIds);
    }
}
