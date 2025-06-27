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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
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
class JpaAiModelSettingsDao extends JpaAbstractDao<AiModelSettingsEntity, AiModelSettings> implements AiModelSettingsDao {

    private final AiModelSettingsRepository aiModelSettingsRepository;

    @Override
    public Optional<AiModelSettings> findByTenantIdAndId(TenantId tenantId, AiModelSettingsId settingsId) {
        return aiModelSettingsRepository.findByTenantIdAndId(tenantId.getId(), settingsId.getId()).map(DaoUtil::getData);
    }

    @Override
    public AiModelSettings findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(aiModelSettingsRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public AiModelSettings findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(aiModelSettingsRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<AiModelSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AiModelSettings> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(aiModelSettingsRepository.findByTenantId(
                tenantId, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), toPageRequest(pageLink))
        );
    }

    @Override
    public PageData<AiModelSettingsId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(aiModelSettingsRepository.findIdsByTenantId(tenantId, toPageRequest(pageLink)).map(AiModelSettingsId::new));
    }

    private static PageRequest toPageRequest(PageLink pageLink) {
        Sort sort;
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder == null) {
            sort = Sort.by(Sort.Direction.ASC, "id");
        } else {
            sort = JpaSort.unsafe(
                    Sort.Direction.fromString(sortOrder.getDirection().name()),
                    AiModelSettingsEntity.COLUMN_MAP.getOrDefault(sortOrder.getProperty(), sortOrder.getProperty())
            );
        }
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), sort);
    }

    @Override
    public AiModelSettingsId getExternalIdByInternal(AiModelSettingsId internalId) {
        return aiModelSettingsRepository.getExternalIdById(internalId.getId()).map(AiModelSettingsId::new).orElse(null);
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
