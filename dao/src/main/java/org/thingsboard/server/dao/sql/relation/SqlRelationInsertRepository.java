/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@Transactional
public class SqlRelationInsertRepository implements RelationInsertRepository {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JPA = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
            " VALUES (:fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, :additionalInfo) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = :additionalInfo returning *";

    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JDBC = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
            " VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = ?";


    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

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
        return (RelationEntity) getQuery(entity, INSERT_ON_CONFLICT_DO_UPDATE_JPA).getSingleResult();
    }

    @Override
    public void saveOrUpdate(List<RelationEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_ON_CONFLICT_DO_UPDATE_JDBC, new BatchPreparedStatementSetter() {
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
        });
    }

}
