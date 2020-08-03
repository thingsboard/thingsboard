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
package org.thingsboard.server.dao.sql.event;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.util.HsqlDao;

import javax.persistence.Query;

@HsqlDao
@Repository
public class HsqlEventInsertRepository extends AbstractEventInsertRepository {

    private static final String P_KEY_CONFLICT_STATEMENT = "(event.id=I.id)";
    private static final String UNQ_KEY_CONFLICT_STATEMENT = "(event.tenant_id=I.tenant_id AND event.entity_type=I.entity_type AND event.entity_id=I.entity_id AND event.event_type=I.event_type AND event.event_uid=I.event_uid)";

    private static final String INSERT_OR_UPDATE_ON_P_KEY_CONFLICT = getInsertString(P_KEY_CONFLICT_STATEMENT);
    private static final String INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT = getInsertString(UNQ_KEY_CONFLICT_STATEMENT);

    @Override
    public EventEntity saveOrUpdate(EventEntity entity) {
        return saveAndGet(entity, INSERT_OR_UPDATE_ON_P_KEY_CONFLICT, INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT);
    }

    @Override
    protected EventEntity doProcessSaveOrUpdate(EventEntity entity, String query) {
        getQuery(entity, query).executeUpdate();
        return entityManager.find(EventEntity.class, entity.getUuid());
    }

    protected Query getQuery(EventEntity entity, String query) {
        return entityManager.createNativeQuery(query, EventEntity.class)
                .setParameter("id", entity.getUuid().toString())
                .setParameter("created_time", entity.getCreatedTime())
                .setParameter("body", entity.getBody().toString())
                .setParameter("entity_id", entity.getEntityId().toString())
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("event_type", entity.getEventType())
                .setParameter("event_uid", entity.getEventUid())
                .setParameter("tenant_id", entity.getTenantId().toString())
                .setParameter("ts", entity.getTs());
    }

    private static String getInsertString(String conflictStatement) {
        return "MERGE INTO event USING (VALUES UUID(:id), :created_time, :body, UUID(:entity_id), :entity_type, :event_type, :event_uid, UUID(:tenant_id), :ts) I (id, created_time, body, entity_id, entity_type, event_type, event_uid, tenant_id, ts) ON " + conflictStatement
                + " WHEN MATCHED THEN UPDATE SET event.id = I.id, event.created_time = I.created_time, event.body = I.body, event.entity_id = I.entity_id, event.entity_type = I.entity_type, event.event_type = I.event_type, event.event_uid = I.event_uid, event.tenant_id = I.tenant_id, event.ts = I.ts" +
                " WHEN NOT MATCHED THEN INSERT (id, created_time, body, entity_id, entity_type, event_type, event_uid, tenant_id, ts) VALUES (I.id, I.created_time, I.body, I.entity_id, I.entity_type, I.event_type, I.event_uid, I.tenant_id, I.ts)";
    }
}