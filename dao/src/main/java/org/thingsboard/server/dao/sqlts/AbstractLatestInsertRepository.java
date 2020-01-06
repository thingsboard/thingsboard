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
package org.thingsboard.server.dao.sqlts;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvLatestEntity;

import java.util.List;

@Repository
public abstract class AbstractLatestInsertRepository extends AbstractInsertRepository {

    public abstract void saveOrUpdate(TsKvLatestEntity entity);

    public abstract void saveOrUpdate(List<TsKvLatestEntity> entities);

    protected void processSaveOrUpdate(TsKvLatestEntity entity, String requestBoolValue, String requestStrValue, String requestLongValue, String requestDblValue) {
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
    protected abstract void saveOrUpdateBoolean(TsKvLatestEntity entity, String query);

    @Modifying
    protected abstract void saveOrUpdateString(TsKvLatestEntity entity, String query);

    @Modifying
    protected abstract void saveOrUpdateLong(TsKvLatestEntity entity, String query);

    @Modifying
    protected abstract void saveOrUpdateDouble(TsKvLatestEntity entity, String query);

}