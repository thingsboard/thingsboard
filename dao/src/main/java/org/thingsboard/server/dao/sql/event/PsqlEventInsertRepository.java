/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@SqlDao
@PsqlDao
@Repository
public class PsqlEventInsertRepository extends EventInsertRepository {

    private static final String UPDATE_P_KEY = "id = :id";
    private static final String UPDATE_UNQ_KEY = "tenant_id = :tenant_id, entity_type = :entity_type, entity_id = :entity_id, event_type = :event_type, event_uid = :event_uid";

    private static final String EVENT_P_KEY = "(id)";
    private static final String EVENT_UNQ_KEY = "(tenant_id, entity_type, entity_id, event_type, event_uid)";

    private static final String FIRST_INSERT_STATEMENT = getInsertOrUpdateString(EVENT_P_KEY, UPDATE_UNQ_KEY);
    private static final String SECOND_INSERT_STATEMENT = getInsertOrUpdateString(EVENT_UNQ_KEY, UPDATE_P_KEY);

    @Override
    public EventEntity saveOrUpdate(EventEntity entity) {
        return getEventEntity(entity, FIRST_INSERT_STATEMENT, SECOND_INSERT_STATEMENT);
    }

    @Override
    protected EventEntity doProcessSaveOrUpdate(EventEntity entity, String strQuery) {
        return (EventEntity) getQuery(entity, strQuery).getSingleResult();

    }

    private static String getInsertOrUpdateString(String eventKey, String updateKey) {
        return "INSERT INTO event (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) VALUES (:id, :body, :entity_id, :entity_type, :event_type, :event_uid, :tenant_id) ON CONFLICT " + eventKey + " DO UPDATE SET body = :body, " + updateKey + " returning *";
    }
}