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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationInsertRepository relationInsertRepository;

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return service.submit(() -> findAllByFrom(tenantId, from, typeGroup));
    }

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        return service.submit(() -> findAllByTo(tenantId, to, typeGroup));
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
    public ListenableFuture<List<EntityRelation>> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<Boolean> checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(() -> relationRepository.existsById(key));
    }

    @Override
    public ListenableFuture<EntityRelation> getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(() -> DaoUtil.getData(relationRepository.findById(key)));
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
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        return relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null;
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        return service.submit(() -> relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null);
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    private boolean deleteRelationIfExists(RelationCompositeKey key) {
        boolean relationExistsBeforeDelete = relationRepository.existsById(key);
        if (relationExistsBeforeDelete) {
            try {
                relationRepository.deleteById(key);
            } catch (DataAccessException e) {
                log.debug("[{}] Concurrency exception while deleting relation", key, e);
            }
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public boolean deleteOutboundRelations(TenantId tenantId, EntityId entity) {
        boolean relationExistsBeforeDelete = false;
        try {
            relationExistsBeforeDelete = relationRepository
                    .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                    .size() > 0;
            if (relationExistsBeforeDelete) {
                relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
            }
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity) {
        return service.submit(
                () -> {
                    boolean relationExistsBeforeDelete = relationRepository
                            .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                            .size() > 0;
                    if (relationExistsBeforeDelete) {
                        relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
                    }
                    return relationExistsBeforeDelete;
                });
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit) {
        return DaoUtil.convertDataList(relationRepository.findRuleNodeToRuleChainRelations(ruleChainType, PageRequest.of(0, limit)));
    }
}
