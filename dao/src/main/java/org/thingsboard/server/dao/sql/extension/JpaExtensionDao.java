// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.extension;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.extension.ExtensionDao;
import org.thingsboard.server.dao.model.sql.ExtensionEntity;
import org.thingsboard.server.dao.sql.HasSecretsEntityDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SqlDao
@Component
@RequiredArgsConstructor
public class JpaExtensionDao extends JpaAbstractDao<ExtensionEntity, Extension> implements ExtensionDao, HasSecretsEntityDao {

    private final ExtensionRepository extensionRepository;

    @Override
    public List<EntityInfo> findByTenantIdAndSecretPlaceholder(TenantId tenantId, String placeholder) {
        return extensionRepository.findByTenantIdAndSecretPlaceholder(tenantId.getId(), placeholder);
    }

    @Override
    public Extension findByTenantIdAndId(TenantId tenantId, ExtensionId extensionId) {
        return DaoUtil.getData(extensionRepository.findByTenantIdAndId(tenantId.getId(), extensionId.getId()).orElse(null));
    }

    @Override
    public Extension findBySlug(String slug) {
        return DaoUtil.getData(extensionRepository.findBySlug(slug));
    }

    @Override
    public List<Extension> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return DaoUtil.convertDataList(extensionRepository.findByIdIn(ids));
    }

    @Override
    public Extension findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(extensionRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public Extension findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(extensionRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<Extension> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Extension> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(extensionRepository.findByTenantId(
                tenantId, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<ExtensionInfo> findInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(extensionRepository.findByTenantId(
                        tenantId, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), DaoUtil.toPageable(pageLink)))
                .mapData(extension -> new ExtensionInfo(extension));
    }

    @Override
    public PageData<Extension> findAll(PageLink pageLink) {
        return DaoUtil.toPageData(extensionRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<ExtensionId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(extensionRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink)).map(ExtensionId::new));
    }

    @Override
    public ExtensionId getExternalIdByInternal(ExtensionId internalId) {
        return extensionRepository.getExternalIdById(internalId.getId()).map(ExtensionId::new).orElse(null);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return extensionRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, ExtensionId extensionId) {
        return extensionRepository.deleteByTenantIdAndIdIn(tenantId.getId(), Set.of(extensionId.getId())) > 0;
    }

    @Override
    public int deleteByTenantIdAndIds(TenantId tenantId, Set<UUID> ids) {
        return extensionRepository.deleteByTenantIdAndIdIn(tenantId.getId(), ids);
    }

    @Override
    public boolean updateStateIf(TenantId tenantId, ExtensionId extensionId, ExtensionState expectedState,
                                 ExtensionState newState, String lastError, Long lastErrorAt) {
        // The state column is mapped as EnumType.STRING, so a native statement matches on the name.
        return extensionRepository.updateStateIf(tenantId.getId(), extensionId.getId(), expectedState.name(),
                newState.name(), lastError, lastErrorAt) > 0;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EXTENSION;
    }

    @Override
    protected Class<ExtensionEntity> getEntityClass() {
        return ExtensionEntity.class;
    }

    @Override
    protected JpaRepository<ExtensionEntity, UUID> getRepository() {
        return extensionRepository;
    }

}
