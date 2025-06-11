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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
class AiSettingsServiceImpl extends CachedVersionedEntityService<AiSettingsCacheKey, AiSettings, AiSettingsCacheEvictEvent> implements AiSettingsService {

    private final DataValidator<AiSettings> aiSettingsValidator;

    private final JpaExecutorService jpaExecutor;
    private final AiSettingsDao aiSettingsDao;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AiSettingsCacheEvictEvent event) {
        cache.evict(event.keys());
    }

    @Override
    @Transactional
    public AiSettings save(AiSettings aiSettings) {
        AiSettings oldSettings = aiSettingsValidator.validate(aiSettings, AiSettings::getTenantId);

        AiSettings savedSettings;
        try {
            savedSettings = aiSettingsDao.saveAndFlush(aiSettings.getTenantId(), aiSettings);
        } catch (Exception e) {
            checkConstraintViolation(e, "ai_settings_name_unq_key", "AI settings record with such name already exists!");
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

        publishEvictEvent(AiSettingsCacheEvictEvent.of(savedSettings.getTenantId(), savedSettings.getId()));

        return savedSettings;
    }

    @Override
    public Optional<AiSettings> findAiSettingsById(TenantId tenantId, AiSettingsId aiSettingsId) {
        return Optional.ofNullable(aiSettingsDao.findById(tenantId, aiSettingsId.getId()));
    }

    @Override
    public PageData<AiSettings> findAiSettingsByTenantId(TenantId tenantId, PageLink pageLink) {
        validatePageLink(pageLink);
        return aiSettingsDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public Optional<AiSettings> findAiSettingsByTenantIdAndId(TenantId tenantId, AiSettingsId aiSettingsId) {
        var cacheKey = AiSettingsCacheKey.of(tenantId, aiSettingsId);
        return Optional.ofNullable(cache.get(cacheKey, () -> aiSettingsDao.findByTenantIdAndId(tenantId, aiSettingsId).orElse(null)));
    }

    @Override
    public FluentFuture<Optional<AiSettings>> findAiSettingsByTenantIdAndIdAsync(TenantId tenantId, AiSettingsId aiSettingsId) {
        return FluentFuture.from(jpaExecutor.submit(() -> findAiSettingsByTenantIdAndId(tenantId, aiSettingsId)));
    }

    @Override
    @Transactional
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiSettingsId aiSettingsId) {
        return deleteByTenantIdAndIdInternal(tenantId, aiSettingsId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return findAiSettingsByTenantIdAndId(tenantId, (AiSettingsId) entityId)
                .map(aiSettings -> aiSettings); // necessary to cast to HasId<?>
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return aiSettingsDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteByTenantIdAndIdInternal(tenantId, new AiSettingsId(id.getId()));
    }

    private boolean deleteByTenantIdAndIdInternal(TenantId tenantId, AiSettingsId aiSettingsId) {
        Optional<AiSettings> aiSettingsOpt = aiSettingsDao.findByTenantIdAndId(tenantId, aiSettingsId);
        if (aiSettingsOpt.isEmpty()) {
            return false;
        }
        boolean deleted = aiSettingsDao.deleteByTenantIdAndId(tenantId, aiSettingsId);
        if (deleted) {
            publishDeleteEvent(aiSettingsOpt.get());
            publishEvictEvent(AiSettingsCacheEvictEvent.of(tenantId, aiSettingsId));
        }
        return deleted;
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        List<AiSettings> deletedSettings = aiSettingsDao.findAllByTenantId(tenantId, new PageLink(Integer.MAX_VALUE)).getData();
        if (deletedSettings.isEmpty()) {
            return;
        }

        aiSettingsDao.deleteByTenantId(tenantId);

        Set<AiSettingsCacheKey> cacheKeys = Sets.newHashSetWithExpectedSize(deletedSettings.size());
        deletedSettings.forEach(settings -> {
            publishDeleteEvent(settings);
            cacheKeys.add(AiSettingsCacheKey.of(settings.getTenantId(), settings.getId()));
        });

        publishEvictEvent(new AiSettingsCacheEvictEvent(cacheKeys));
    }

    private void publishDeleteEvent(AiSettings settings) {
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(settings.getTenantId())
                .entityId(settings.getId())
                .entity(settings)
                .build());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_SETTINGS;
    }

}
