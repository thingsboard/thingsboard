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
package org.thingsboard.server.dao.sqlts;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.AbsractTsKvEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public abstract class AbstractTimeseriesInsertRepository<T extends AbsractTsKvEntity> {

    protected static final String BOOL_V = "bool_v";
    protected static final String STR_V = "str_v";
    protected static final String LONG_V = "long_v";
    protected static final String DBL_V = "dbl_v";

    @PersistenceContext
    protected EntityManager entityManager;

    public abstract void saveOrUpdate(T entity);

    protected void processSaveOrUpdate(T entity, String requestBoolValue, String requestStrValue, String requestLongValue, String requestDblValue) {
        if (entity.getBooleanValue() != null) {
            saveOrUpdateBoolean(entity, requestBoolValue);
        }
        if (entity.getStrValue() != null) {
            saveOrUpdateString(entity, requestStrValue);
        }
        if (entity.getLongValue() != null) {
            saveOrUpdateLong(entity, requestLongValue);
        }
        if (entity.getDoubleValue() != null) {
            saveOrUpdateDouble(entity, requestDblValue);
        }
    }

    @Modifying
    protected abstract void saveOrUpdateBoolean(T entity, String query);

    @Modifying
    protected abstract void saveOrUpdateString(T entity, String query);

    @Modifying
    protected abstract void saveOrUpdateLong(T entity, String query);

    @Modifying
    protected abstract void saveOrUpdateDouble(T entity, String query);

}