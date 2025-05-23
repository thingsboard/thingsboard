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
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.ai.AiSettingsDao;
import org.thingsboard.server.dao.model.sql.AiSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaAiSettingsDao extends JpaAbstractDao<AiSettingsEntity, AiSettings> implements AiSettingsDao {

    private final AiSettingsRepository aiSettingsRepository;

    @Override
    public Optional<AiSettings> findByTenantIdAndId(TenantId tenantId, AiSettingsId aiSettingsId) {
        return aiSettingsRepository.findByTenantIdAndId(tenantId.getId(), aiSettingsId.getId()).map(DaoUtil::getData);
    }

    @Override
    public PageData<AiSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(aiSettingsRepository.findByTenantId(
                tenantId.getId(), StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), DaoUtil.toPageable(pageLink))
        );
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return aiSettingsRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteById(TenantId tenantId, AiSettingsId aiSettingsId) {
        return aiSettingsRepository.deleteByIdIn(Set.of(aiSettingsId.getId())) > 0;
    }

    @Override
    public int deleteByTenantId(TenantId tenantId) {
        return aiSettingsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiSettingsId aiSettingsId) {
        return aiSettingsRepository.deleteByTenantIdAndIdIn(tenantId.getId(), Set.of(aiSettingsId.getId())) > 0;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_SETTINGS;
    }

    @Override
    protected Class<AiSettingsEntity> getEntityClass() {
        return AiSettingsEntity.class;
    }

    @Override
    protected JpaRepository<AiSettingsEntity, UUID> getRepository() {
        return aiSettingsRepository;
    }

}
