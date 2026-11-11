// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.relation;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

@Repository
@Transactional
public class SqlRelationInsertRepository implements RelationInsertRepository {

    private static final String SEQ_VERSION = "nextval('relation_version_seq')";
    private static final String CITUS_INSERT_VERSION = "1";
    private static final String CITUS_UPDATE_VERSION = "relation.version + 1";

    // Built via String.format: %1$s = INSERT-row version, %2$s = ON CONFLICT update version. Literal % must be escaped as %%.
    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JPA_TEMPLATE = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, version, additional_info)" +
            " VALUES (:fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, %1$s, :additionalInfo) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = :additionalInfo, version = %2$s returning *";

    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JDBC_TEMPLATE = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, version, additional_info)" +
            " VALUES (?, ?, ?, ?, ?, ?, %1$s, ?) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = ?, version = %2$s";

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private CitusSettings citusSettings;

    @Getter
    private String insertJpaQuery;
    @Getter
    private String insertJdbcQuery;

    @PostConstruct
    private void initQueries() {
        boolean citus = citusSettings.isEnabled();
        String insertVersion = citus ? CITUS_INSERT_VERSION : SEQ_VERSION;
        String updateVersion = citus ? CITUS_UPDATE_VERSION : SEQ_VERSION;
        this.insertJpaQuery = String.format(INSERT_ON_CONFLICT_DO_UPDATE_JPA_TEMPLATE, insertVersion, updateVersion);
        this.insertJdbcQuery = String.format(INSERT_ON_CONFLICT_DO_UPDATE_JDBC_TEMPLATE, insertVersion, updateVersion);
    }

    protected Query getQuery(RelationEntity entity, String query) {
        Query nativeQuery = entityManager.createNativeQuery(query, RelationEntity.class);
        if (entity.getAdditionalInfo() == null) {
            nativeQuery.setParameter("additionalInfo", null);
        } else {
            nativeQuery.setParameter("additionalInfo", JacksonUtil.toString(entity.getAdditionalInfo()));
        }
        return nativeQuery
                .setParameter("fromId", entity.getFromId())
                .setParameter("fromType", entity.getFromType())
                .setParameter("toId", entity.getToId())
                .setParameter("toType", entity.getToType())
                .setParameter("relationTypeGroup", entity.getRelationTypeGroup())
                .setParameter("relationType", entity.getRelationType());
    }

    @Override
    public RelationEntity saveOrUpdate(RelationEntity entity) {
        return (RelationEntity) getQuery(entity, insertJpaQuery).getSingleResult();
    }

    @Override
    public List<RelationEntity> saveOrUpdate(List<RelationEntity> entities) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.batchUpdate(new SequencePreparedStatementCreator(insertJdbcQuery), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RelationEntity relation = entities.get(i);
                ps.setObject(1, relation.getFromId());
                ps.setString(2, relation.getFromType());
                ps.setObject(3, relation.getToId());
                ps.setString(4, relation.getToType());

                ps.setString(5, relation.getRelationTypeGroup());
                ps.setString(6, relation.getRelationType());

                if (relation.getAdditionalInfo() == null) {
                    ps.setString(7, null);
                    ps.setString(8, null);
                } else {
                    String json = JacksonUtil.toString(relation.getAdditionalInfo());
                    ps.setString(7, json);
                    ps.setString(8, json);
                }
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        }, keyHolder);

        var seqNumbers = keyHolder.getKeyList();

        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).setVersion((Long) seqNumbers.get(i).get(VERSION_COLUMN));
        }

        return entities;
    }

    private record SequencePreparedStatementCreator(String sql) implements PreparedStatementCreator, SqlProvider {

        private static final String[] COLUMNS = {VERSION_COLUMN};

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(sql, COLUMNS);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }

}
