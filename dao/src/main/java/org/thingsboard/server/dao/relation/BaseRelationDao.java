/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.relation;

import com.datastax.driver.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.AbstractAsyncDao;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashvayka on 25.04.17.
 */
@Component
@Slf4j
public class BaseRelationDao extends AbstractAsyncDao implements RelationDao {

    private static final String SELECT_COLUMNS = "SELECT " +
            ModelConstants.RELATION_FROM_ID_PROPERTY + "," +
            ModelConstants.RELATION_FROM_TYPE_PROPERTY + "," +
            ModelConstants.RELATION_TO_ID_PROPERTY + "," +
            ModelConstants.RELATION_TO_TYPE_PROPERTY + "," +
            ModelConstants.RELATION_TYPE_PROPERTY + "," +
            ModelConstants.ADDITIONAL_INFO_PROPERTY;
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String AND = " AND ";

    private PreparedStatement saveStmt;
    private PreparedStatement findAllByFromStmt;
    private PreparedStatement findAllByFromAndTypeStmt;
    private PreparedStatement findAllByToStmt;
    private PreparedStatement findAllByToAndTypeStmt;
    private PreparedStatement checkRelationStmt;
    private PreparedStatement deleteStmt;
    private PreparedStatement deleteAllByEntityStmt;

    @PostConstruct
    public void init() {
        super.startExecutor();
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFrom(EntityId from) {
        BoundStatement stmt = getFindAllByFromStmt().bind().setUUID(0, from.getId()).setString(1, from.getEntityType().name());
        return executeAsyncRead(from, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAndType(EntityId from, String relationType) {
        BoundStatement stmt = getFindAllByFromAndTypeStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setString(2, relationType);
        return executeAsyncRead(from, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByTo(EntityId to) {
        BoundStatement stmt = getFindAllByToStmt().bind().setUUID(0, to.getId()).setString(1, to.getEntityType().name());
        return executeAsyncRead(to, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByToAndType(EntityId to, String relationType) {
        BoundStatement stmt = getFindAllByToAndTypeStmt().bind()
                .setUUID(0, to.getId())
                .setString(1, to.getEntityType().name())
                .setString(2, relationType);
        return executeAsyncRead(to, stmt);
    }

    @Override
    public ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType) {
        BoundStatement stmt = getCheckRelationStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setUUID(2, to.getId())
                .setString(3, to.getEntityType().name())
                .setString(4, relationType);
        return getFuture(executeAsyncRead(stmt), rs -> rs != null ? rs.one() != null : false);
    }

    @Override
    public ListenableFuture<Boolean> saveRelation(EntityRelation relation) {
        BoundStatement stmt = getSaveStmt().bind()
                .setUUID(0, relation.getFrom().getId())
                .setString(1, relation.getFrom().getEntityType().name())
                .setUUID(2, relation.getTo().getId())
                .setString(3, relation.getTo().getEntityType().name())
                .setString(4, relation.getType())
                .set(5, relation.getAdditionalInfo(), JsonNode.class);
        ResultSetFuture future = executeAsyncWrite(stmt);
        return getBooleanListenableFuture(future);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityRelation relation) {
        return deleteRelation(relation.getFrom(), relation.getTo(), relation.getType());
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityId from, EntityId to, String relationType) {
        BoundStatement stmt = getDeleteStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setUUID(2, to.getId())
                .setString(3, to.getEntityType().name())
                .setString(4, relationType);
        ResultSetFuture future = executeAsyncWrite(stmt);
        return getBooleanListenableFuture(future);
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelations(EntityId entity) {
        BoundStatement stmt = getDeleteAllByEntityStmt().bind()
                .setUUID(0, entity.getId())
                .setString(1, entity.getEntityType().name());
        ResultSetFuture future = executeAsyncWrite(stmt);
        return getBooleanListenableFuture(future);
    }

    private PreparedStatement getSaveStmt() {
        if (saveStmt == null) {
            saveStmt = getSession().prepare("INSERT INTO " + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    "(" + ModelConstants.RELATION_FROM_ID_PROPERTY +
                    "," + ModelConstants.RELATION_FROM_TYPE_PROPERTY +
                    "," + ModelConstants.RELATION_TO_ID_PROPERTY +
                    "," + ModelConstants.RELATION_TO_TYPE_PROPERTY +
                    "," + ModelConstants.RELATION_TYPE_PROPERTY +
                    "," + ModelConstants.ADDITIONAL_INFO_PROPERTY + ")" +
                    " VALUES(?, ?, ?, ?, ?, ?)");
        }
        return saveStmt;
    }

    private PreparedStatement getDeleteStmt() {
        if (deleteStmt == null) {
            deleteStmt = getSession().prepare("DELETE FROM " + ModelConstants.RELATION_COLUMN_FAMILY_NAME +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ?" +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ?" +
                    AND + ModelConstants.RELATION_TO_ID_PROPERTY + " = ?" +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ?" +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ?");
        }
        return deleteStmt;
    }

    private PreparedStatement getDeleteAllByEntityStmt() {
        if (deleteAllByEntityStmt == null) {
            deleteAllByEntityStmt = getSession().prepare("DELETE FROM " + ModelConstants.RELATION_COLUMN_FAMILY_NAME +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ?" +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ?");
        }
        return deleteAllByEntityStmt;
    }

    private PreparedStatement getFindAllByFromStmt() {
        if (findAllByFromStmt == null) {
            findAllByFromStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ? ");
        }
        return findAllByFromStmt;
    }

    private PreparedStatement getFindAllByFromAndTypeStmt() {
        if (findAllByFromAndTypeStmt == null) {
            findAllByFromAndTypeStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ? ");
        }
        return findAllByFromAndTypeStmt;
    }

    private PreparedStatement getFindAllByToStmt() {
        if (findAllByToStmt == null) {
            findAllByToStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_REVERSE_VIEW_NAME + " " +
                    WHERE + ModelConstants.RELATION_TO_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ? ");
        }
        return findAllByToStmt;
    }

    private PreparedStatement getFindAllByToAndTypeStmt() {
        if (findAllByToAndTypeStmt == null) {
            findAllByToAndTypeStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_REVERSE_VIEW_NAME + " " +
                    WHERE + ModelConstants.RELATION_TO_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ? ");
        }
        return findAllByToAndTypeStmt;
    }

    private PreparedStatement getCheckRelationStmt() {
        if (checkRelationStmt == null) {
            checkRelationStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ? ");
        }
        return checkRelationStmt;
    }

    private EntityRelation getEntityRelation(Row row) {
        EntityRelation relation = new EntityRelation();
        relation.setType(row.getString(ModelConstants.RELATION_TYPE_PROPERTY));
        relation.setAdditionalInfo(row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY, JsonNode.class));
        relation.setFrom(toEntity(row, ModelConstants.RELATION_FROM_ID_PROPERTY, ModelConstants.RELATION_FROM_TYPE_PROPERTY));
        relation.setTo(toEntity(row, ModelConstants.RELATION_TO_ID_PROPERTY, ModelConstants.RELATION_TO_TYPE_PROPERTY));
        return relation;
    }

    private EntityId toEntity(Row row, String uuidColumn, String typeColumn) {
        return EntityIdFactory.getByTypeAndUuid(row.getString(typeColumn), row.getUUID(uuidColumn));
    }

    private ListenableFuture<List<EntityRelation>> executeAsyncRead(EntityId from, BoundStatement stmt) {
        log.debug("Generated query [{}] for entity {}", stmt, from);
        return getFuture(executeAsyncRead(stmt), rs -> {
            List<Row> rows = rs.all();
            List<EntityRelation> entries = new ArrayList<>(rows.size());
            if (!rows.isEmpty()) {
                rows.forEach(row -> {
                    entries.add(getEntityRelation(row));
                });
            }
            return entries;
        });
    }

    private ListenableFuture<Boolean> getBooleanListenableFuture(ResultSetFuture rsFuture) {
        return getFuture(rsFuture, rs -> rs != null ? rs.wasApplied() : false);
    }

    private <T> ListenableFuture<T> getFuture(ResultSetFuture future, java.util.function.Function<ResultSet, T> transformer) {
        return Futures.transform(future, new Function<ResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable ResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

}
