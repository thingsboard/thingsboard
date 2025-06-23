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
package org.thingsboard.server.dao.sql.ai;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.ai.AiModelSettingsDao;
import org.thingsboard.server.dao.model.sql.AiModelSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaAiSettingsDao extends JpaAbstractDao<AiModelSettingsEntity, AiModelSettings> implements AiModelSettingsDao {

    private final AiModelSettingsRepository aiModelSettingsRepository;

    @Override
    public Optional<AiModelSettings> findByTenantIdAndId(TenantId tenantId, AiModelSettingsId settingsId) {
        return aiModelSettingsRepository.findByTenantIdAndId(tenantId.getId(), settingsId.getId()).map(DaoUtil::getData);
    }

    @Override
    public PageData<AiModelSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(aiModelSettingsRepository.findByTenantId(
                tenantId.getId(), StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), DaoUtil.toPageable(pageLink, AiModelSettingsEntity.COLUMN_MAP))
        );
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return aiModelSettingsRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteById(TenantId tenantId, AiModelSettingsId settingsId) {
        return aiModelSettingsRepository.deleteByIdIn(Set.of(settingsId.getId())) > 0;
    }

    @Override
    public int deleteByTenantId(TenantId tenantId) {
        return aiModelSettingsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiModelSettingsId settingsId) {
        return aiModelSettingsRepository.deleteByTenantIdAndIdIn(tenantId.getId(), Set.of(settingsId.getId())) > 0;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL_SETTINGS;
    }

    @Override
    protected Class<AiModelSettingsEntity> getEntityClass() {
        return AiModelSettingsEntity.class;
    }

    @Override
    protected JpaRepository<AiModelSettingsEntity, UUID> getRepository() {
        return aiModelSettingsRepository;
    }

}
