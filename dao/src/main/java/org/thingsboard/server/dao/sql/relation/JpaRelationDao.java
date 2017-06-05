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
package org.thingsboard.server.dao.sql.relation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaRelationDao implements RelationDao {

    @Autowired
    private RelationRepository relationRepository;

    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFrom(EntityId from, RelationTypeGroup typeGroup) {
        return executorService.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAndType(EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return executorService.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByTo(EntityId to, RelationTypeGroup typeGroup) {
        return executorService.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByToAndType(EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return executorService.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key =
                new RelationCompositeKey(from.getId(),
                        from.getEntityType().name(),
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name());
        return executorService.submit(() -> relationRepository.findOne(key) != null);
    }

    @Override
    public ListenableFuture<Boolean> saveRelation(EntityRelation relation) {
        return executorService.submit(() -> relationRepository.save(new RelationEntity(relation)) != null);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return executorService.submit(
                () -> {
                    relationRepository.delete(key);
                    return !relationRepository.exists(key);
                });
    }

    @Override
    public ListenableFuture<Boolean> deleteRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key =
                new RelationCompositeKey(from.getId(),
                        from.getEntityType().name(),
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name());
        return executorService.submit(
                () -> {
                    boolean result = relationRepository.exists(key);
                    relationRepository.delete(key);
                    return result;
                });
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelations(EntityId entity) {
        RelationEntity relationEntity = new RelationEntity();
        relationEntity.setFromId(entity.getId());
        relationEntity.setFromType(entity.getEntityType().name());

        return executorService.submit(
                () -> {
                    boolean result = relationRepository
                            .findAllByFromIdAndFromType(relationEntity.getFromId(), relationEntity.getFromType())
                            .size() > 0;
                    relationRepository.delete(relationEntity);
                    return result;
                });
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findRelations(EntityId from, String relationType, RelationTypeGroup typeGroup, EntityType childType, TimePageLink pageLink) {
//        executorService.submit(() -> DaoUtil.convertDataList(
//                relationRepository.findRelations(
//                        to.getId(),
//                        to.getEntityType().name(),
//                        relationType,
//                        typeGroup.name())));
        return null;
    }


}
