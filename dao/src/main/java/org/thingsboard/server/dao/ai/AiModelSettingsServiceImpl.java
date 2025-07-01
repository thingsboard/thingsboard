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
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.model.sql.AiModelSettingsEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
class AiModelSettingsServiceImpl extends CachedVersionedEntityService<AiModelSettingsCacheKey, AiModelSettings, AiModelSettingsCacheEvictEvent> implements AiModelSettingsService {

    private final DataValidator<AiModelSettings> aiModelSettingsValidator;

    private final JpaExecutorService jpaExecutor;
    private final AiModelSettingsDao aiModelSettingsDao;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AiModelSettingsCacheEvictEvent event) {
        var cacheKey = event.cacheKey();
        if (event instanceof AiModelSettingsCacheEvictEvent.Saved savedEvent) {
            cache.put(cacheKey, savedEvent.savedSettings());
        } else if (event instanceof AiModelSettingsCacheEvictEvent.Deleted) {
            cache.evict(cacheKey);
        } else {
            throw new UnsupportedOperationException("Unsupported event type: " + event.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional
    public AiModelSettings save(AiModelSettings settings) {
        AiModelSettings oldSettings = aiModelSettingsValidator.validate(settings, AiModelSettings::getTenantId);

        AiModelSettings savedSettings;
        try {
            savedSettings = aiModelSettingsDao.saveAndFlush(settings.getTenantId(), settings);
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "ai_model_settings_name_unq_key", "AI model settings with such name already exist!",
                    "ai_model_settings_external_id_unq_key", "AI model settings with such external ID already exist!");
            throw e;
        }

        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(savedSettings.getTenantId())
                .entity(savedSettings)
                .oldEntity(oldSettings)
                .entityId(savedSettings.getId())
                .created(oldSettings == null)
                .broadcastEvent(true)
                .build());

        var cacheKey = AiModelSettingsCacheKey.of(savedSettings.getTenantId(), savedSettings.getId());
        publishEvictEvent(new AiModelSettingsCacheEvictEvent.Saved(cacheKey, savedSettings));

        return savedSettings;
    }

    @Override
    public Optional<AiModelSettings> findAiModelSettingsById(TenantId tenantId, AiModelSettingsId settingsId) {
        return Optional.ofNullable(aiModelSettingsDao.findById(tenantId, settingsId.getId()));
    }

    @Override
    public PageData<AiModelSettings> findAiModelSettingsByTenantId(TenantId tenantId, PageLink pageLink) {
        validatePageLink(pageLink, AiModelSettingsEntity.ALLOWED_SORT_PROPERTIES);
        return aiModelSettingsDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public Optional<AiModelSettings> findAiModelSettingsByTenantIdAndId(TenantId tenantId, AiModelSettingsId settingsId) {
        var cacheKey = AiModelSettingsCacheKey.of(tenantId, settingsId);
        return Optional.ofNullable(cache.get(cacheKey, () -> aiModelSettingsDao.findByTenantIdAndId(tenantId, settingsId).orElse(null)));
    }

    @Override
    public FluentFuture<Optional<AiModelSettings>> findAiModelSettingsByTenantIdAndIdAsync(TenantId tenantId, AiModelSettingsId settingsId) {
        return FluentFuture.from(jpaExecutor.submit(() -> findAiModelSettingsByTenantIdAndId(tenantId, settingsId)));
    }

    @Override
    @Transactional
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiModelSettingsId settingsId) {
        return deleteByTenantIdAndIdInternal(tenantId, settingsId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return findAiModelSettingsByTenantIdAndId(tenantId, (AiModelSettingsId) entityId)
                .map(settings -> settings); // necessary to cast to HasId<?>
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return aiModelSettingsDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteByTenantIdAndIdInternal(tenantId, new AiModelSettingsId(id.getId()));
    }

    private boolean deleteByTenantIdAndIdInternal(TenantId tenantId, AiModelSettingsId settingsId) {
        Optional<AiModelSettings> toDeleteOpt = aiModelSettingsDao.findByTenantIdAndId(tenantId, settingsId);
        if (toDeleteOpt.isEmpty()) {
            return false;
        }
        boolean deleted = aiModelSettingsDao.deleteByTenantIdAndId(tenantId, settingsId);
        if (deleted) {
            publishDeleteEvent(toDeleteOpt.get());
            publishEvictEvent(new AiModelSettingsCacheEvictEvent.Deleted(AiModelSettingsCacheKey.of(tenantId, settingsId)));
        }
        return deleted;
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        List<AiModelSettings> toDelete = aiModelSettingsDao.findAllByTenantId(tenantId, new PageLink(Integer.MAX_VALUE)).getData();
        if (toDelete.isEmpty()) {
            return;
        }

        aiModelSettingsDao.deleteByTenantId(tenantId);

        toDelete.forEach(settings -> {
            publishDeleteEvent(settings);
            publishEvictEvent(new AiModelSettingsCacheEvictEvent.Deleted(AiModelSettingsCacheKey.of(settings.getTenantId(), settings.getId())));
        });
    }

    private void publishDeleteEvent(AiModelSettings settings) {
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(settings.getTenantId())
                .entityId(settings.getId())
                .entity(settings)
                .build());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL_SETTINGS;
    }

}
