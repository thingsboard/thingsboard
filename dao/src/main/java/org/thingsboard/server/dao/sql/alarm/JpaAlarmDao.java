/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private AlarmRepository alarmRepository;

    @Override
    protected Class getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return alarmRepository;
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        ListenableFuture<Alarm> listenableFuture = service.submit(() -> DaoUtil.getData(
                alarmRepository.findLatestByOriginatorAndType(tenantId.getId(), originator.getId(),
                originator.getEntityType().ordinal(), type)));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(UUID key) {
        //TODO: Implement
        return null;
    }

    @Override
    public ListenableFuture<List<Alarm>> findAlarms(AlarmQuery query) {
        //TODO: Implement
        return null;
    }
}
