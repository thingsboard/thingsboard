/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.dao.attributes;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.dao.AbstractDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.timeseries.BaseTimeseriesDao;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.*;
import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
public class BaseAttributesDao extends AbstractDao implements AttributesDao {
    
    private PreparedStatement saveStmt;

    @Override
    public AttributeKvEntry find(EntityId entityId, String attributeType, String attributeKey) {
        Select.Where select = select().from(ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType))
                .and(eq(ATTRIBUTE_KEY_COLUMN, attributeKey));
        log.trace("Generated query [{}] for entityId {} and key {}", select, entityId, attributeKey);
        return convertResultToAttributesKvEntry(attributeKey, executeRead(select).one());
    }

    @Override
    public List<AttributeKvEntry> findAll(EntityId entityId, String attributeType) {
        Select.Where select = select().from(ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType));
        log.trace("Generated query [{}] for entityId {} and attributeType {}", select, entityId, attributeType);
        return convertResultToAttributesKvEntryList(executeRead(select));
    }

    @Override
    public ResultSetFuture save(EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        BoundStatement stmt = getSaveStmt().bind();
        stmt.setString(0, entityId.getEntityType().name());
        stmt.setUUID(1, entityId.getId());
        stmt.setString(2, attributeType);
        stmt.setString(3, attribute.getKey());
        stmt.setLong(4, attribute.getLastUpdateTs());
        stmt.setString(5, attribute.getStrValue().orElse(null));
        if (attribute.getBooleanValue().isPresent()) {
            stmt.setBool(6, attribute.getBooleanValue().get());
        } else {
            stmt.setToNull(6);
        }
        if (attribute.getLongValue().isPresent()) {
            stmt.setLong(7, attribute.getLongValue().get());
        } else {
            stmt.setToNull(7);
        }
        if (attribute.getDoubleValue().isPresent()) {
            stmt.setDouble(8, attribute.getDoubleValue().get());
        } else {
            stmt.setToNull(8);
        }
        return executeAsyncWrite(stmt);
    }

    @Override
    public void removeAll(EntityId entityId, String attributeType, List<String> keys) {
        for (String key : keys) {
            delete(entityId, attributeType, key);
        }
    }

    private void delete(EntityId entityId, String attributeType, String key) {
        Statement delete = QueryBuilder.delete().all().from(ModelConstants.ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType))
                .and(eq(ATTRIBUTE_KEY_COLUMN, key));
        log.debug("Remove request: {}", delete.toString());
        getSession().execute(delete);
    }

    private PreparedStatement getSaveStmt() {
        if (saveStmt == null) {
            saveStmt = getSession().prepare("INSERT INTO " + ModelConstants.ATTRIBUTES_KV_CF +
                    "(" + ENTITY_TYPE_COLUMN +
                    "," + ENTITY_ID_COLUMN +
                    "," + ATTRIBUTE_TYPE_COLUMN +
                    "," + ATTRIBUTE_KEY_COLUMN +
                    "," + LAST_UPDATE_TS_COLUMN +
                    "," + ModelConstants.STRING_VALUE_COLUMN +
                    "," + ModelConstants.BOOLEAN_VALUE_COLUMN +
                    "," + ModelConstants.LONG_VALUE_COLUMN +
                    "," + ModelConstants.DOUBLE_VALUE_COLUMN +
                    ")" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }
        return saveStmt;
    }

    private AttributeKvEntry convertResultToAttributesKvEntry(String key, Row row) {
        AttributeKvEntry attributeEntry = null;
        if (row != null) {
            long lastUpdateTs = row.get(LAST_UPDATE_TS_COLUMN, Long.class);
            attributeEntry = new BaseAttributeKvEntry(BaseTimeseriesDao.toKvEntry(row, key), lastUpdateTs);
        }
        return attributeEntry;
    }

    private List<AttributeKvEntry> convertResultToAttributesKvEntryList(ResultSet resultSet) {
        List<Row> rows = resultSet.all();
        List<AttributeKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> {
                String key = row.getString(ModelConstants.ATTRIBUTE_KEY_COLUMN);
                AttributeKvEntry kvEntry = convertResultToAttributesKvEntry(key, row);
                if (kvEntry != null) {
                    entries.add(kvEntry);
                }
            });
        }
        return entries;
    }

}
