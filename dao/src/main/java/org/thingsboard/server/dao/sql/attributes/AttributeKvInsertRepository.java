/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.dao.AbstractVersionedInsertRepository;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
public class AttributeKvInsertRepository extends AbstractVersionedInsertRepository<AttributeKvEntity> {

    private static final String BATCH_UPDATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = nextval('attribute_kv_version_seq') " +
            "WHERE entity_id = ? and attribute_type =? and attribute_key = ? RETURNING version;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts, version) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?,  cast(? AS json), ?, nextval('attribute_kv_version_seq')) " +
                    "ON CONFLICT (entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = nextval('attribute_kv_version_seq') RETURNING version;";

    @Override
    protected void setOnBatchUpdateValues(PreparedStatement ps, int i, List<AttributeKvEntity> entities) throws SQLException {
        AttributeKvEntity kvEntity = entities.get(i);
        ps.setString(1, replaceNullChars(kvEntity.getStrValue()));

        if (kvEntity.getLongValue() != null) {
            ps.setLong(2, kvEntity.getLongValue());
        } else {
            ps.setNull(2, Types.BIGINT);
        }

        if (kvEntity.getDoubleValue() != null) {
            ps.setDouble(3, kvEntity.getDoubleValue());
        } else {
            ps.setNull(3, Types.DOUBLE);
        }

        if (kvEntity.getBooleanValue() != null) {
            ps.setBoolean(4, kvEntity.getBooleanValue());
        } else {
            ps.setNull(4, Types.BOOLEAN);
        }

        ps.setString(5, replaceNullChars(kvEntity.getJsonValue()));

        ps.setLong(6, kvEntity.getLastUpdateTs());
        ps.setObject(7, kvEntity.getId().getEntityId());
        ps.setInt(8, kvEntity.getId().getAttributeType());
        ps.setInt(9, kvEntity.getId().getAttributeKey());
    }

    @Override
    protected void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<AttributeKvEntity> insertEntities) throws SQLException {
        AttributeKvEntity kvEntity = insertEntities.get(i);
        ps.setObject(1, kvEntity.getId().getEntityId());
        ps.setInt(2, kvEntity.getId().getAttributeType());
        ps.setInt(3, kvEntity.getId().getAttributeKey());

        ps.setString(4, replaceNullChars(kvEntity.getStrValue()));
        ps.setString(10, replaceNullChars(kvEntity.getStrValue()));

        if (kvEntity.getLongValue() != null) {
            ps.setLong(5, kvEntity.getLongValue());
            ps.setLong(11, kvEntity.getLongValue());
        } else {
            ps.setNull(5, Types.BIGINT);
            ps.setNull(11, Types.BIGINT);
        }

        if (kvEntity.getDoubleValue() != null) {
            ps.setDouble(6, kvEntity.getDoubleValue());
            ps.setDouble(12, kvEntity.getDoubleValue());
        } else {
            ps.setNull(6, Types.DOUBLE);
            ps.setNull(12, Types.DOUBLE);
        }

        if (kvEntity.getBooleanValue() != null) {
            ps.setBoolean(7, kvEntity.getBooleanValue());
            ps.setBoolean(13, kvEntity.getBooleanValue());
        } else {
            ps.setNull(7, Types.BOOLEAN);
            ps.setNull(13, Types.BOOLEAN);
        }

        ps.setString(8, replaceNullChars(kvEntity.getJsonValue()));
        ps.setString(14, replaceNullChars(kvEntity.getJsonValue()));

        ps.setLong(9, kvEntity.getLastUpdateTs());
        ps.setLong(15, kvEntity.getLastUpdateTs());
    }

    @Override
    protected String getBatchUpdateQuery() {
        return BATCH_UPDATE;
    }

    @Override
    protected String getInsertOrUpdateQuery() {
        return INSERT_OR_UPDATE;
    }
}
