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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.thingsboard.server.common.data.UUIDConverter;
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

    private static final String INSERT_FIRST_STATEMENT = getInsertOrUpdateString(EVENT_UNQ_KEY, UPDATE_P_KEY);
    private static final String INSERT_SECOND_STATEMENT = getInsertOrUpdateString(EVENT_P_KEY, UPDATE_UNQ_KEY);

    @Override
    public EventEntity saveOrUpdate(EventEntity entity) {
        EventEntity eventEntity = null;
        TransactionStatus insertTransaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            eventEntity = processSaveOrUpdate(entity, INSERT_SECOND_STATEMENT);
            transactionManager.commit(insertTransaction);
        } catch (Throwable e) {
            transactionManager.rollback(insertTransaction);
            if (e.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Entity with entityId {} and entityType {}", e.getMessage(), entity.getEventUid(), entity.getEventType());
                TransactionStatus transaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    eventEntity = processSaveOrUpdate(entity, INSERT_FIRST_STATEMENT);
                } catch (Throwable th) {
                    log.trace("Could not execute the update statement for Entity with entityId {} and entityType {}", entity.getEventUid(), entity.getEventType());
                    transactionManager.rollback(transaction);
                }
                transactionManager.commit(transaction);
            } else {
                log.trace("Could not execute the insert statement for Entity with entityId {} and entityType {}", entity.getEventUid(), entity.getEventType());
            }
        }
        return eventEntity;
    }

    @Override
    protected EventEntity doProcessSaveOrUpdate(EventEntity entity, String query) {
        return (EventEntity) entityManager.createNativeQuery(query, EventEntity.class)
                .setParameter("id", UUIDConverter.fromTimeUUID(entity.getId()))
                .setParameter("body", entity.getBody().toString())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("event_type", entity.getEventType())
                .setParameter("event_uid", entity.getEventUid())
                .setParameter("tenant_id", entity.getTenantId())
                .getSingleResult();

    }

    private static String getInsertOrUpdateString(String eventKey, String updatePKey) {
        return "INSERT INTO event (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) VALUES (:id, :body, :entity_id, :entity_type, :event_type, :event_uid, :tenant_id) ON CONFLICT " + eventKey + " DO UPDATE SET body = :body, " + updatePKey + " returning *";
    }

}