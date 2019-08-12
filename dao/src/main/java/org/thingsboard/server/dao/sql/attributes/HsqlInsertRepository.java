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
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@SqlDao
@HsqlDao
@Repository
public class HsqlInsertRepository extends AttributeKvInsertRepository {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String INSERT_BOOL_STATEMENT = getInsertString(BOOL_V);
    private static final String INSERT_STR_STATEMENT = getInsertString(STR_V);
    private static final String INSERT_LONG_STATEMENT = getInsertString(LONG_V);
    private static final String INSERT_DBL_STATEMENT = getInsertString(DBL_V);

    private static final String WHERE_STATEMENT = " WHERE entity_type = :entity_type AND entity_id = :entity_id AND attribute_type = :attribute_type AND attribute_key = :attribute_key";

    private static final String UPDATE_BOOL_STATEMENT = getUpdateString(BOOL_V);
    private static final String UPDATE_STR_STATEMENT = getUpdateString(STR_V);
    private static final String UPDATE_LONG_STATEMENT = getUpdateString(LONG_V);
    private static final String UPDATE_DBL_STATEMENT = getUpdateString(DBL_V);

    @Override
    public void saveOrUpdate(AttributeKvEntity entity) {
        DefaultTransactionDefinition insertDefinition = new DefaultTransactionDefinition();
        insertDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus insertTransaction = transactionManager.getTransaction(insertDefinition);
        try {
            processSaveOrUpdate(entity, INSERT_BOOL_STATEMENT, INSERT_STR_STATEMENT, INSERT_LONG_STATEMENT, INSERT_DBL_STATEMENT);
            transactionManager.commit(insertTransaction);
        } catch (Throwable e) {
            transactionManager.rollback(insertTransaction);
            if (e.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Entity with entityId {} and entityType {}", e.getMessage(), UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
                DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
                definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                TransactionStatus transaction = transactionManager.getTransaction(definition);
                try {
                    processSaveOrUpdate(entity, UPDATE_BOOL_STATEMENT, UPDATE_STR_STATEMENT, UPDATE_LONG_STATEMENT, UPDATE_DBL_STATEMENT);
                } catch (Throwable th) {
                    log.trace("Could not execute the update statement for Entity with entityId {} and entityType {}", UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
                    transactionManager.rollback(transaction);
                }
                transactionManager.commit(transaction);
            } else {
                log.trace("Could not execute the insert statement for Entity with entityId {} and entityType {}", UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
            }
        }
    }

    private static String getInsertString(String value) {
        return "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :" + value + ", :last_update_ts)";
    }

    private static String getUpdateString(String value) {
        return "UPDATE attribute_kv SET " + value + " = :" + value + ", last_update_ts = :last_update_ts" + WHERE_STATEMENT;
    }
}