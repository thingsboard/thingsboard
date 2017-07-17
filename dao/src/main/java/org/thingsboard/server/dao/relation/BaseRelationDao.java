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
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.type.RelationTypeGroupCodec;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * Created by ashvayka on 25.04.17.
 */
@Component
@Slf4j
@NoSqlDao
public class BaseRelationDao extends CassandraAbstractAsyncDao implements RelationDao {

    private static final String SELECT_COLUMNS = "SELECT " +
            ModelConstants.RELATION_FROM_ID_PROPERTY + "," +
            ModelConstants.RELATION_FROM_TYPE_PROPERTY + "," +
            ModelConstants.RELATION_TO_ID_PROPERTY + "," +
            ModelConstants.RELATION_TO_TYPE_PROPERTY + "," +
            ModelConstants.RELATION_TYPE_GROUP_PROPERTY + "," +
            ModelConstants.RELATION_TYPE_PROPERTY + "," +
            ModelConstants.ADDITIONAL_INFO_PROPERTY;
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String AND = " AND ";

    private static final RelationTypeGroupCodec relationTypeGroupCodec = new RelationTypeGroupCodec();

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
    public ListenableFuture<List<EntityRelation>> findAllByFrom(EntityId from, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getFindAllByFromStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .set(2, typeGroup, relationTypeGroupCodec);
        return executeAsyncRead(from, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAndType(EntityId from, String relationType, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getFindAllByFromAndTypeStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .set(2, typeGroup, relationTypeGroupCodec)
                .setString(3, relationType);
        return executeAsyncRead(from, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByTo(EntityId to, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getFindAllByToStmt().bind()
                .setUUID(0, to.getId())
                .setString(1, to.getEntityType().name())
                .set(2, typeGroup, relationTypeGroupCodec);
        return executeAsyncRead(to, stmt);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByToAndType(EntityId to, String relationType, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getFindAllByToAndTypeStmt().bind()
                .setUUID(0, to.getId())
                .setString(1, to.getEntityType().name())
                .set(2, typeGroup, relationTypeGroupCodec)
                .setString(3, relationType);
        return executeAsyncRead(to, stmt);
    }

    @Override
    public ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getCheckRelationStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setUUID(2, to.getId())
                .setString(3, to.getEntityType().name())
                .set(4, typeGroup, relationTypeGroupCodec)
                .setString(5, relationType);
        return getFuture(executeAsyncRead(stmt), rs -> rs != null ? rs.one() != null : false);
    }

    @Override
    public ListenableFuture<EntityRelation> getRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getCheckRelationStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setUUID(2, to.getId())
                .setString(3, to.getEntityType().name())
                .set(4, typeGroup, relationTypeGroupCodec)
                .setString(5, relationType);
        return getFuture(executeAsyncRead(stmt), rs -> rs != null ? getEntityRelation(rs.one()) : null);
    }


    @Override
    public ListenableFuture<Boolean> saveRelation(EntityRelation relation) {
        BoundStatement stmt = getSaveStmt().bind()
                .setUUID(0, relation.getFrom().getId())
                .setString(1, relation.getFrom().getEntityType().name())
                .setUUID(2, relation.getTo().getId())
                .setString(3, relation.getTo().getEntityType().name())
                .set(4, relation.getTypeGroup(), relationTypeGroupCodec)
                .setString(5, relation.getType())
                .set(6, relation.getAdditionalInfo(), JsonNode.class);
        ResultSetFuture future = executeAsyncWrite(stmt);
        return getBooleanListenableFuture(future);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityRelation relation) {
        return deleteRelation(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        BoundStatement stmt = getDeleteStmt().bind()
                .setUUID(0, from.getId())
                .setString(1, from.getEntityType().name())
                .setUUID(2, to.getId())
                .setString(3, to.getEntityType().name())
                .set(4, typeGroup, relationTypeGroupCodec)
                .setString(5, relationType);
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

    @Override
    public ListenableFuture<List<EntityRelation>> findRelations(EntityId from, String relationType, RelationTypeGroup typeGroup, EntityType childType, TimePageLink pageLink) {
        Select.Where query = CassandraAbstractSearchTimeDao.buildQuery(ModelConstants.RELATION_BY_TYPE_AND_CHILD_TYPE_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.RELATION_FROM_ID_PROPERTY, from.getId()),
                        eq(ModelConstants.RELATION_FROM_TYPE_PROPERTY, from.getEntityType().name()),
                        eq(ModelConstants.RELATION_TYPE_GROUP_PROPERTY, typeGroup.name()),
                        eq(ModelConstants.RELATION_TYPE_PROPERTY, relationType),
                        eq(ModelConstants.RELATION_TO_TYPE_PROPERTY, childType.name())),
                Arrays.asList(
                        pageLink.isAscOrder() ? QueryBuilder.desc(ModelConstants.RELATION_TYPE_GROUP_PROPERTY) :
                                QueryBuilder.asc(ModelConstants.RELATION_TYPE_GROUP_PROPERTY),
                        pageLink.isAscOrder() ? QueryBuilder.desc(ModelConstants.RELATION_TYPE_PROPERTY) :
                                QueryBuilder.asc(ModelConstants.RELATION_TYPE_PROPERTY),
                        pageLink.isAscOrder() ? QueryBuilder.desc(ModelConstants.RELATION_TO_TYPE_PROPERTY) :
                                QueryBuilder.asc(ModelConstants.RELATION_TO_TYPE_PROPERTY)
                ),
                pageLink, ModelConstants.RELATION_TO_ID_PROPERTY);
        return getFuture(executeAsyncRead(query), this::getEntityRelations);
    }

    private PreparedStatement getSaveStmt() {
        if (saveStmt == null) {
            saveStmt = getSession().prepare("INSERT INTO " + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    "(" + ModelConstants.RELATION_FROM_ID_PROPERTY +
                    "," + ModelConstants.RELATION_FROM_TYPE_PROPERTY +
                    "," + ModelConstants.RELATION_TO_ID_PROPERTY +
                    "," + ModelConstants.RELATION_TO_TYPE_PROPERTY +
                    "," + ModelConstants.RELATION_TYPE_GROUP_PROPERTY +
                    "," + ModelConstants.RELATION_TYPE_PROPERTY +
                    "," + ModelConstants.ADDITIONAL_INFO_PROPERTY + ")" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?)");
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
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ?" +
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
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ? ");
        }
        return findAllByFromStmt;
    }

    private PreparedStatement getFindAllByFromAndTypeStmt() {
        if (findAllByFromAndTypeStmt == null) {
            findAllByFromAndTypeStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_COLUMN_FAMILY_NAME + " " +
                    WHERE + ModelConstants.RELATION_FROM_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_FROM_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ? ");
        }
        return findAllByFromAndTypeStmt;
    }


    private PreparedStatement getFindAllByToStmt() {
        if (findAllByToStmt == null) {
            findAllByToStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_REVERSE_VIEW_NAME + " " +
                    WHERE + ModelConstants.RELATION_TO_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ? ");
        }
        return findAllByToStmt;
    }

    private PreparedStatement getFindAllByToAndTypeStmt() {
        if (findAllByToAndTypeStmt == null) {
            findAllByToAndTypeStmt = getSession().prepare(SELECT_COLUMNS + " " +
                    FROM + ModelConstants.RELATION_REVERSE_VIEW_NAME + " " +
                    WHERE + ModelConstants.RELATION_TO_ID_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TO_TYPE_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ? " +
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
                    AND + ModelConstants.RELATION_TYPE_GROUP_PROPERTY + " = ? " +
                    AND + ModelConstants.RELATION_TYPE_PROPERTY + " = ? ");
        }
        return checkRelationStmt;
    }

    private EntityId toEntity(Row row, String uuidColumn, String typeColumn) {
        return EntityIdFactory.getByTypeAndUuid(row.getString(typeColumn), row.getUUID(uuidColumn));
    }

    private ListenableFuture<List<EntityRelation>> executeAsyncRead(EntityId from, BoundStatement stmt) {
        log.debug("Generated query [{}] for entity {}", stmt, from);
        return getFuture(executeAsyncRead(stmt), rs -> getEntityRelations(rs));
    }

    private ListenableFuture<Boolean> getBooleanListenableFuture(ResultSetFuture rsFuture) {
        return getFuture(rsFuture, rs -> rs != null ? rs.wasApplied() : false);
    }

    private List<EntityRelation> getEntityRelations(ResultSet rs) {
        List<Row> rows = rs.all();
        List<EntityRelation> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> {
                entries.add(getEntityRelation(row));
            });
        }
        return entries;
    }

    private EntityRelation getEntityRelation(Row row) {
        EntityRelation relation = new EntityRelation();
        relation.setTypeGroup(row.get(ModelConstants.RELATION_TYPE_GROUP_PROPERTY, relationTypeGroupCodec));
        relation.setType(row.getString(ModelConstants.RELATION_TYPE_PROPERTY));
        relation.setAdditionalInfo(row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY, JsonNode.class));
        relation.setFrom(toEntity(row, ModelConstants.RELATION_FROM_ID_PROPERTY, ModelConstants.RELATION_FROM_TYPE_PROPERTY));
        relation.setTo(toEntity(row, ModelConstants.RELATION_TO_ID_PROPERTY, ModelConstants.RELATION_TO_TYPE_PROPERTY));
        return relation;
    }

}
