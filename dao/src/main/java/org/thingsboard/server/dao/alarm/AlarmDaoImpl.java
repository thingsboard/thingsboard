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
package org.thingsboard.server.dao.alarm;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.CassandraAbstractModelDao;
import org.thingsboard.server.dao.model.AlarmEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
public class AlarmDaoImpl extends CassandraAbstractModelDao<AlarmEntity, Alarm> implements AlarmDao {

    @Override
    protected Class<AlarmEntity> getColumnFamilyClass() {
        return AlarmEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ALARM_COLUMN_FAMILY_NAME;
    }

    @Override
    public Alarm save(Alarm alarm) {
        log.debug("Save asset [{}] ", alarm);
        return super.save(alarm);
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        Select select = select().from(ALARM_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(ALARM_TENANT_ID_PROPERTY, tenantId.getId()));
        query.and(eq(ALARM_ORIGINATOR_ID_PROPERTY, originator.getId()));
        query.and(eq(ALARM_ORIGINATOR_TYPE_PROPERTY, originator.getEntityType()));
        query.and(eq(ALARM_TYPE_PROPERTY, type));
        query.limit(1);
        query.orderBy(QueryBuilder.asc(ModelConstants.ALARM_TYPE_PROPERTY), QueryBuilder.desc(ModelConstants.ID_PROPERTY));
        return findOneByStatementAsync(query);
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(UUID key) {
        log.debug("Get alarm by id {}", key);
        Select.Where query = select().from(ALARM_BY_ID_VIEW_NAME).where(eq(ModelConstants.ID_PROPERTY, key));
        query.limit(1);
        log.trace("Execute query {}", query);
        return findOneByStatementAsync(query);
    }
}
