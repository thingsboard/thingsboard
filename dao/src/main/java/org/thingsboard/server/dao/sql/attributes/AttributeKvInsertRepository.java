// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.attributes;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.AbstractVersionedInsertRepository;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
public class AttributeKvInsertRepository extends AbstractVersionedInsertRepository<AttributeKvEntity> {

    @Autowired
    private CitusSettings citusSettings;

    private static final String SEQ_VERSION = "nextval('attribute_kv_version_seq')";
    private static final String INCREMENT_VERSION = "attribute_kv.version + 1";

    // The *_TEMPLATE strings below are resolved via String.format in initQueries(); any literal '%' added
    // to this SQL must be escaped as '%%' or String.format will throw at startup.
    private static final String BATCH_UPDATE_TEMPLATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = %s " +
            "WHERE entity_id = ? and attribute_type =? and attribute_key = ? RETURNING version;";

    private static final String INSERT_OR_UPDATE_TEMPLATE =
            "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts, version) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?,  cast(? AS json), ?, %1$s) " +
                    "ON CONFLICT (entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?, version = %2$s RETURNING version;";

    private String batchUpdateQuery;
    private String insertOrUpdateQuery;

    @PostConstruct
    private void initQueries() {
        boolean citus = citusSettings.isEnabled();
        String insertVersion = citus ? CITUS_INSERT_VERSION : SEQ_VERSION;
        String updateVersion = citus ? INCREMENT_VERSION : SEQ_VERSION;
        this.batchUpdateQuery = String.format(BATCH_UPDATE_TEMPLATE, updateVersion);
        this.insertOrUpdateQuery = String.format(INSERT_OR_UPDATE_TEMPLATE, insertVersion, updateVersion);
    }

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
        return batchUpdateQuery;
    }

    @Override
    protected String getInsertOrUpdateQuery() {
        return insertOrUpdateQuery;
    }
}
