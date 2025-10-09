/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.model.ModelConstants.RELATION_FROM_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_FROM_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TO_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TO_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TYPE_GROUP_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    private static final List<String> ALL_TYPE_GROUP_NAMES = new ArrayList<>();
    private static final String RETURNING = "RETURNING from_id, from_type, to_id, to_type, relation_type, relation_type_group, nextval('relation_version_seq') as version";
    private static final String DELETE_QUERY = "DELETE FROM relation WHERE from_id = ? AND from_type = ? AND to_id = ? AND to_type = ? AND relation_type = ? AND relation_type_group = ? " + RETURNING;

    static {
        Arrays.stream(RelationTypeGroup.values()).map(RelationTypeGroup::name).forEach(ALL_TYPE_GROUP_NAMES::add);
    }

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationInsertRepository relationInsertRepository;

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroupIn(
                        from.getId(),
                        from.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    @Override
    public List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroupIn(
                        to.getId(),
                        to.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    @Override
    public List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }


    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> checkRelation(tenantId, from, to, relationType, typeGroup));
    }

    @Override
    public boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return relationRepository.existsById(key);
    }

    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return DaoUtil.getData(relationRepository.findById(key));
    }

    private RelationCompositeKey getRelationCompositeKey(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return new RelationCompositeKey(from.getId(),
                from.getEntityType().name(),
                to.getId(),
                to.getEntityType().name(),
                relationType,
                typeGroup.name());
    }

    @Override
    public EntityRelation saveRelation(TenantId tenantId, EntityRelation relation) {
        return DaoUtil.getData(relationInsertRepository.saveOrUpdate(new RelationEntity(relation)));
    }

    @Override
    public List<EntityRelation> saveRelations(TenantId tenantId, List<EntityRelation> relations) {
        List<RelationEntity> entities = relations.stream().map(RelationEntity::new).collect(Collectors.toList());
        return DaoUtil.convertDataList(relationInsertRepository.saveOrUpdate(entities));
    }

    @Override
    public ListenableFuture<EntityRelation> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        return service.submit(() -> DaoUtil.getData(relationInsertRepository.saveOrUpdate(new RelationEntity(relation))));
    }

    @Override
    public EntityRelation deleteRelation(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<EntityRelation> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    @Override
    public EntityRelation deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<EntityRelation> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    private EntityRelation deleteRelationIfExists(RelationCompositeKey key) {
        return jdbcTemplate.query(DELETE_QUERY, rs -> {
            if (!rs.next()) {
                return null;
            }
            EntityRelation relation = new EntityRelation();

            var fromId = rs.getObject(RELATION_FROM_ID_PROPERTY, UUID.class);
            var fromType = rs.getString(RELATION_FROM_TYPE_PROPERTY);
            var toId = rs.getObject(RELATION_TO_ID_PROPERTY, UUID.class);
            var toType = rs.getString(RELATION_TO_TYPE_PROPERTY);
            var relationTypeGroup = rs.getString(RELATION_TYPE_GROUP_PROPERTY);
            var relationType = rs.getString(RELATION_TYPE_PROPERTY);
            var version = rs.getLong(VERSION_COLUMN);

            //additionalInfo ignored (no need to send extra data for delete events)

            relation.setTo(EntityIdFactory.getByTypeAndUuid(toType, toId));
            relation.setFrom(EntityIdFactory.getByTypeAndUuid(fromType, fromId));
            relation.setType(relationType);
            relation.setTypeGroup(RelationTypeGroup.valueOf(relationTypeGroup));
            relation.setVersion(version);
            return relation;
        }, key.getFromId(), key.getFromType(), key.getToId(), key.getToType(), key.getRelationType(), key.getRelationTypeGroup());
    }

    @Override
    public List<EntityRelation> deleteOutboundRelations(TenantId tenantId, EntityId entity) {
        return deleteRelations(entity, null, false);
    }

    @Override
    public List<EntityRelation> deleteOutboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup) {
        return deleteRelations(entity, Collections.singletonList(relationTypeGroup.name()), false);
    }

    @Override
    public List<EntityRelation> deleteInboundRelations(TenantId tenantId, EntityId entity) {
        return deleteRelations(entity, ALL_TYPE_GROUP_NAMES, true);
    }

    @Override
    public List<EntityRelation> deleteInboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup) {
        return deleteRelations(entity, Collections.singletonList(relationTypeGroup.name()), true);
    }

    private List<EntityRelation> deleteRelations(EntityId entityId, List<String> relationTypeGroups, boolean inbound) {
        List<Object> params = new ArrayList<>();
        params.add(entityId.getId());
        params.add(entityId.getEntityType().name());

        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM relation WHERE ");
        if (inbound) {
            sqlBuilder.append("to_id = ? AND to_type = ? ");
        } else {
            sqlBuilder.append("from_id = ? AND from_type = ? ");
        }

        if (!CollectionUtils.isEmpty(relationTypeGroups)) {
            sqlBuilder.append("AND relation_type_group IN (?");
            for (int i = 1; i < relationTypeGroups.size(); i++) {
                sqlBuilder.append(", ?");
            }
            sqlBuilder.append(")");
            params.addAll(relationTypeGroups);
        }

        sqlBuilder.append(RETURNING);

        return jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray()).stream()
                .map(row -> {
                    EntityRelation relation = new EntityRelation();

                    var fromId = row.get(RELATION_FROM_ID_PROPERTY);
                    var fromType = row.get(RELATION_FROM_TYPE_PROPERTY);
                    var toId = row.get(RELATION_TO_ID_PROPERTY);
                    var toType = row.get(RELATION_TO_TYPE_PROPERTY);
                    var relationTypeGroup = row.get(RELATION_TYPE_GROUP_PROPERTY);
                    var relationType = row.get(RELATION_TYPE_PROPERTY);
                    var version = row.get(VERSION_COLUMN);

                    //additionalInfo ignored (no need to send extra data for delete events)

                    relation.setTo(EntityIdFactory.getByTypeAndUuid((String) toType, (UUID) toId));
                    relation.setFrom(EntityIdFactory.getByTypeAndUuid((String) fromType, (UUID) fromId));
                    relation.setType((String) relationType);
                    relation.setTypeGroup(RelationTypeGroup.valueOf((String) relationTypeGroup));
                    relation.setVersion((Long) version);
                    return relation;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit) {
        return DaoUtil.convertDataList(relationRepository.findRuleNodeToRuleChainRelations(ruleChainType, PageRequest.of(0, limit)));
    }

    @Override
    public List<EntityRelation> findByRelationPathQuery(TenantId tenantId, EntityRelationPathQuery query, int limit) {
        List<RelationPathLevel> levels = query.levels();
        if (levels == null || levels.isEmpty()) {
            return List.of();
        }
        if (limit <= 0) {
            return List.of();
        }
        String sql = buildRelationPathSql(query);
        Object[] params = buildRelationPathParams(query, limit);

        log.trace("[{}] relation path query: {}", tenantId, sql);

        return jdbcTemplate.queryForList(sql, params).stream()
                .map(row -> {
                    var entityRelation = new EntityRelation();
                    var fromId = (UUID) row.get(RELATION_FROM_ID_PROPERTY);
                    var fromType = (String) row.get(RELATION_FROM_TYPE_PROPERTY);
                    var toId = (UUID) row.get(RELATION_TO_ID_PROPERTY);
                    var toType = (String) row.get(RELATION_TO_TYPE_PROPERTY);
                    var grp = (String) row.get(RELATION_TYPE_GROUP_PROPERTY);
                    var type = (String) row.get(RELATION_TYPE_PROPERTY);
                    var version = (Long) row.get(VERSION_COLUMN);

                    entityRelation.setFrom(EntityIdFactory.getByTypeAndUuid(fromType, fromId));
                    entityRelation.setTo(EntityIdFactory.getByTypeAndUuid(toType, toId));
                    entityRelation.setType(type);
                    entityRelation.setTypeGroup(RelationTypeGroup.valueOf(grp));
                    entityRelation.setVersion(version);
                    return entityRelation;
                })
                .collect(Collectors.toList());
    }

    private Object[] buildRelationPathParams(EntityRelationPathQuery query, int limit) {
        final List<Object> params = new ArrayList<>();
        // seed
        params.add(query.rootEntityId().getId());
        params.add(query.rootEntityId().getEntityType().name());

        // levels
        for (var lvl : query.levels()) {
            params.add(lvl.relationType());
        }

        // limit
        params.add(limit);

        return params.toArray();
    }

    private static String buildRelationPathSql(EntityRelationPathQuery query) {
        List<RelationPathLevel> levels = query.levels();
        StringBuilder sb = new StringBuilder();

        sb.append("WITH seed AS (\n")
                .append("  SELECT ?::uuid AS id, ?::varchar AS type\n")
                .append(")");

        String prev = "seed";
        for (int i = 0; i < levels.size() - 1; i++) {
            RelationPathLevel lvl = levels.get(i);
            boolean down = lvl.direction() == EntitySearchDirection.FROM;

            String cur = "lvl" + (i + 1);
            String joinCond = down
                    ? "r.from_id = p.id AND r.from_type = p.type"
                    : "r.to_id   = p.id AND r.to_type   = p.type";
            String selectNext = down
                    ? "r.to_id   AS id, r.to_type   AS type"
                    : "r.from_id AS id, r.from_type AS type";

            sb.append(",\n").append(cur).append(" AS (\n")
                    .append("  SELECT ").append(selectNext).append("\n")
                    .append("  FROM ").append(RELATION_TABLE_NAME).append(" r\n")
                    .append("  JOIN ").append(prev).append(" p ON ").append(joinCond).append("\n")
                    .append("  WHERE r.relation_type_group = '").append(RelationTypeGroup.COMMON).append("'\n")
                    .append("    AND r.relation_type = ?\n")
                    .append(")");
            prev = cur;
        }

        RelationPathLevel last = levels.get(levels.size() - 1);
        boolean lastDown = last.direction() == EntitySearchDirection.FROM;
        String prevForLast = (levels.size() == 1) ? "seed" : prev;
        String lastJoin = lastDown
                ? "r.from_id = p.id AND r.from_type = p.type"
                : "r.to_id   = p.id AND r.to_type   = p.type";

        sb.append("\n")
                .append("SELECT r.from_id, r.from_type, r.to_id, r.to_type,\n")
                .append("       r.relation_type_group, r.relation_type, r.version\n")
                .append("FROM ").append(RELATION_TABLE_NAME).append(" r\n")
                .append("JOIN ").append(prevForLast).append(" p ON ").append(lastJoin).append("\n")
                .append("WHERE r.relation_type_group = '").append(RelationTypeGroup.COMMON).append("'\n")
                .append("  AND r.relation_type = ?\n")
                .append("LIMIT ?");

        return sb.toString();
    }

}
