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

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
@HsqlDao
@Repository
@Transactional
public class HsqlAttributesInsertRepository extends AttributeKvInsertRepository {

    private static final String ON_BOOL_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.long_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_STR_VALUE_UPDATE_SET_NULLS = " attribute_kv.bool_v = null, attribute_kv.long_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_LONG_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.bool_v = null, attribute_kv.dbl_v = null ";
    private static final String ON_DBL_VALUE_UPDATE_SET_NULLS = " attribute_kv.str_v = null, attribute_kv.long_v = null, attribute_kv.bool_v = null ";

    private static final String INSERT_BOOL_STATEMENT = getInsertOrUpdateString(BOOL_V, ON_BOOL_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_STR_STATEMENT = getInsertOrUpdateString(STR_V, ON_STR_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_LONG_STATEMENT = getInsertOrUpdateString(LONG_V, ON_LONG_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_DBL_STATEMENT = getInsertOrUpdateString(DBL_V, ON_DBL_VALUE_UPDATE_SET_NULLS);

    @Override
    public void saveOrUpdate(AttributeKvEntity entity) {
        processSaveOrUpdate(entity, INSERT_BOOL_STATEMENT, INSERT_STR_STATEMENT, INSERT_LONG_STATEMENT, INSERT_DBL_STATEMENT);
    }

    private static String getInsertOrUpdateString(String value, String nullValues) {
        return "MERGE INTO attribute_kv USING(VALUES :entity_type, :entity_id, :attribute_type, :attribute_key, :" + value + ", :last_update_ts) A (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) ON (attribute_kv.entity_type=A.entity_type AND attribute_kv.entity_id=A.entity_id AND attribute_kv.attribute_type=A.attribute_type AND attribute_kv.attribute_key=A.attribute_key) WHEN MATCHED THEN UPDATE SET attribute_kv." + value + " = A." + value + ", attribute_kv.last_update_ts = A.last_update_ts," + nullValues + "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) VALUES (A.entity_type, A.entity_id, A.attribute_type, A.attribute_key, A." + value + ", A.last_update_ts)";
    }
}