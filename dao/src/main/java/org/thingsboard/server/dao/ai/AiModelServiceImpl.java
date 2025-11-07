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
package org.thingsboard.server.dao.ai;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.model.sql.AiModelEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
class AiModelServiceImpl extends CachedVersionedEntityService<AiModelCacheKey, AiModel, AiModelCacheEvictEvent> implements AiModelService {

    private final DataValidator<AiModel> aiModelValidator;

    private final JpaExecutorService jpaExecutor;
    private final AiModelDao aiModelDao;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AiModelCacheEvictEvent event) {
        var cacheKey = event.cacheKey();
        if (event instanceof AiModelCacheEvictEvent.Saved savedEvent) {
            cache.put(cacheKey, savedEvent.savedModel());
        } else if (event instanceof AiModelCacheEvictEvent.Deleted) {
            cache.evict(cacheKey);
        } else {
            throw new UnsupportedOperationException("Unsupported event type: " + event.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional
    public AiModel save(AiModel model) {
        return save(model, true);
    }

    @Override
    public AiModel save(AiModel aiModel, boolean doValidate) {
        AiModel oldAiModel = null;
        if (doValidate) {
            oldAiModel = aiModelValidator.validate(aiModel, AiModel::getTenantId);
        } else if (aiModel.getId() != null) {
            oldAiModel = findAiModelById(aiModel.getTenantId(), aiModel.getId()).orElse(null);
        }

        AiModel savedModel;
        try {
            savedModel = aiModelDao.saveAndFlush(aiModel.getTenantId(), aiModel);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedModel.getTenantId()).entityId(savedModel.getId())
                    .entity(savedModel).oldEntity(oldAiModel).created(oldAiModel == null).broadcastEvent(true).build());
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "ai_model_name_unq_key", "AI model with such name already exist!",
                    "ai_model_external_id_unq_key", "AI model with such external ID already exists!");
            throw e;
        }

        var cacheKey = AiModelCacheKey.of(savedModel.getTenantId(), savedModel.getId());
        publishEvictEvent(new AiModelCacheEvictEvent.Saved(cacheKey, savedModel));

        return savedModel;
    }

    @Override
    public Optional<AiModel> findAiModelById(TenantId tenantId, AiModelId modelId) {
        return Optional.ofNullable(aiModelDao.findById(tenantId, modelId.getId()));
    }

    @Override
    public PageData<AiModel> findAiModelsByTenantId(TenantId tenantId, PageLink pageLink) {
        validatePageLink(pageLink, AiModelEntity.ALLOWED_SORT_PROPERTIES);
        return aiModelDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public Optional<AiModel> findAiModelByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        var cacheKey = AiModelCacheKey.of(tenantId, modelId);
        return Optional.ofNullable(cache.get(cacheKey, () -> aiModelDao.findByTenantIdAndId(tenantId, modelId).orElse(null)));
    }

    @Override
    public FluentFuture<Optional<AiModel>> findAiModelByTenantIdAndIdAsync(TenantId tenantId, AiModelId modelId) {
        return FluentFuture.from(jpaExecutor.submit(() -> findAiModelByTenantIdAndId(tenantId, modelId)));
    }

    @Override
    public Optional<AiModel> findAiModelByTenantIdAndName(TenantId tenantId, String name) {
        return Optional.ofNullable(aiModelDao.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    @Transactional
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        return deleteByTenantIdAndIdInternal(tenantId, modelId.getId());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return findAiModelByTenantIdAndId(tenantId, (AiModelId) entityId)
                .map(model -> model); // necessary to cast to HasId<?>
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return findAiModelByTenantIdAndIdAsync(tenantId, new AiModelId(entityId.getId()))
                .transform(modelOpt -> modelOpt.map(model -> model), directExecutor());  // necessary to cast to HasId<?>
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return aiModelDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteByTenantIdAndIdInternal(tenantId, id.getId());
    }

    private boolean deleteByTenantIdAndIdInternal(TenantId tenantId, UUID modelId) {
        AiModel aiModel = findAiModelById(tenantId, new AiModelId(modelId)).orElse(null);
        if (aiModel == null) {
            return false;
        }

        boolean deleted = aiModelDao.deleteByTenantIdAndId(tenantId, aiModel.getId());
        if (deleted) {
            publishEvictEvent(new AiModelCacheEvictEvent.Deleted(AiModelCacheKey.of(tenantId, aiModel.getId())));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(aiModel.getId()).entity(aiModel).build());
        }

        return deleted;
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        Set<AiModelId> deleted = aiModelDao.deleteByTenantId(tenantId);
        deleted.forEach(id -> publishEvictEvent(new AiModelCacheEvictEvent.Deleted(AiModelCacheKey.of(tenantId, id))));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}
