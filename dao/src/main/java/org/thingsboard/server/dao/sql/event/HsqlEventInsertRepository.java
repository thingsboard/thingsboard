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
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@SqlDao
@HsqlDao
@Repository
public class HsqlEventInsertRepository extends EventInsertRepository {

    private static final String EVENT_P_KEY_CONFLICT = "(event.id=I.id)";
    private static final String EVENT_UNQ_KEY_CONFLICT = "(event.tenant_id=I.tenant_id, event.entity_type=I.entity_type, event.entity_id=I.entity_id, event.event_type=I.event_type, event.event_uid=I.event_uid)";

    private static final String FIRST_INSERT_STATEMENT = getInsertString(EVENT_P_KEY_CONFLICT);
    private static final String SECOND_INSERT_STATEMENT = getInsertString(EVENT_UNQ_KEY_CONFLICT);

    @Override
    public EventEntity saveOrUpdate(EventEntity entity) {
        EventEntity eventEntity = null;
        TransactionStatus insertTransaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            eventEntity = processSaveOrUpdate(entity, FIRST_INSERT_STATEMENT);
            transactionManager.commit(insertTransaction);
        } catch (Throwable e) {
            transactionManager.rollback(insertTransaction);
            if (e.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Entity with entityId {} and entityType {}", e.getMessage(), entity.getEventUid(), entity.getEventType());
                TransactionStatus transaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    eventEntity = processSaveOrUpdate(entity, SECOND_INSERT_STATEMENT);
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
        entityManager.createNativeQuery(query, EventEntity.class)
                .setParameter("id", UUIDConverter.fromTimeUUID(entity.getId()))
                .setParameter("body", entity.getBody().toString())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("event_type", entity.getEventType())
                .setParameter("event_uid", entity.getEventUid())
                .setParameter("tenant_id", entity.getTenantId())
                .executeUpdate();
        return entityManager.find(EventEntity.class, UUIDConverter.fromTimeUUID(entity.getId()));
    }

    private static String getInsertString(String conflictStatement) {
        return "MERGE INTO event USING (VALUES :id, :body, :entity_id, :entity_type, :event_type, :event_uid, :tenant_id) I (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) ON " + conflictStatement + " WHEN MATCHED THEN UPDATE SET event.id = I.id, event.body = I.body, event.entity_id = I.entity_id, event.entity_type = I.entity_type, event.event_type = I.event_type, event.event_uid = I.event_uid, event.tenant_id = I.tenant_id" +
                " WHEN NOT MATCHED THEN INSERT (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) VALUES (I.id, I.body, I.entity_id, I.entity_type, I.event_type, I.event_uid, I.tenant_id)";
    }
}