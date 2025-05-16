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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Optional;

import static org.thingsboard.server.dao.entity.AbstractEntityService.checkConstraintViolation;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
class AiSettingsServiceImpl implements AiSettingsService {

    private final AiSettingsDao aiSettingsDao;

    @Override
    public AiSettings save(AiSettings aiSettings) {
        try {
            return aiSettingsDao.saveAndFlush(aiSettings.getTenantId(), aiSettings);
        } catch (Exception e) {
            checkConstraintViolation(e, "ai_settings_name_unq_key", "AI settings record with such name already exists!");
            throw e;
        }
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
        return aiSettingsDao.findByTenantIdAndId(tenantId, aiSettingsId);
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiSettingsId aiSettingsId) {
        return aiSettingsDao.deleteByTenantIdAndId(tenantId, aiSettingsId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(aiSettingsDao.findById(tenantId, entityId.getId()));
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return aiSettingsDao.countByTenantId(tenantId);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        aiSettingsDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        aiSettingsDao.deleteByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_SETTINGS;
    }

}
