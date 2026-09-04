// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.ai;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.ai.AiModelDao;
import org.thingsboard.server.dao.model.sql.AiModelEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaAiModelDao extends JpaAbstractDao<AiModelEntity, AiModel> implements AiModelDao {

    private final AiModelRepository aiModelRepository;

    @Override
    public Optional<AiModel> findByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.findByTenantIdAndId(tenantId.getId(), modelId.getId()).map(DaoUtil::getData);
    }

    @Override
    public AiModel findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(aiModelRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public AiModel findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(aiModelRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<AiModel> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AiModel> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(aiModelRepository.findByTenantId(
                tenantId, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), toPageRequest(pageLink))
        );
    }

    @Override
    public PageData<AiModelId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(aiModelRepository.findIdsByTenantId(tenantId, toPageRequest(pageLink)).map(AiModelId::new));
    }

    private static PageRequest toPageRequest(PageLink pageLink) {
        Sort sort;
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder == null) {
            sort = Sort.by(Sort.Direction.ASC, "id");
        } else {
            sort = JpaSort.unsafe(
                    Sort.Direction.fromString(sortOrder.getDirection().name()),
                    AiModelEntity.COLUMN_MAP.getOrDefault(sortOrder.getProperty(), sortOrder.getProperty())
            ).and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), sort);
    }

    @Override
    public AiModelId getExternalIdByInternal(AiModelId internalId) {
        return aiModelRepository.getExternalIdById(internalId.getId()).map(AiModelId::new).orElse(null);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return aiModelRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteById(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.deleteByIdIn(Set.of(modelId.getId())) > 0;
    }

    @Override
    public Set<AiModelId> deleteByTenantId(TenantId tenantId) {
        return aiModelRepository.deleteByTenantId(tenantId.getId()).stream()
                .map(AiModelId::new)
                .collect(toSet());
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.deleteByTenantIdAndIdIn(tenantId.getId(), Set.of(modelId.getId())) > 0;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

    @Override
    protected Class<AiModelEntity> getEntityClass() {
        return AiModelEntity.class;
    }

    @Override
    protected JpaRepository<AiModelEntity, UUID> getRepository() {
        return aiModelRepository;
    }

}
