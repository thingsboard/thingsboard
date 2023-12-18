/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.resource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TbResourceEntity;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.io.InputStream;
import java.sql.Blob;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaTbResourceDao extends JpaAbstractDao<TbResourceEntity, TbResource> implements TbResourceDao {

    private final TbResourceRepository resourceRepository;

    public JpaTbResourceDao(TbResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    protected Class<TbResourceEntity> getEntityClass() {
        return TbResourceEntity.class;
    }

    @Override
    protected JpaRepository<TbResourceEntity, UUID> getRepository() {
        return resourceRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public TbResource findById(TenantId tenantId, UUID key) {
        return super.findById(tenantId, key);
    }

    @Transactional(readOnly = true)
    @Override
    public TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        return DaoUtil.getData(resourceRepository.findByTenantIdAndResourceTypeAndResourceKey(tenantId.getId(), resourceType.name(), resourceKey));
    }

    @Transactional(readOnly = true)
    @Override
    public PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Transactional(readOnly = true)
    @Override
    public PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                       ResourceType resourceType,
                                                                       PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findResourcesPage(
                tenantId.getId(),
                TenantId.SYS_TENANT_ID.getId(),
                resourceType.name(),
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)
        ));
    }

    @Transactional(readOnly = true)
    @Override
    public List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType,
                                                                   String[] objectIds,
                                                                   String searchText) {
        return objectIds == null ?
                DaoUtil.convertDataList(resourceRepository.findResources(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(),
                        searchText)) :
                DaoUtil.convertDataList(resourceRepository.findResourcesByIds(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(), objectIds));
    }

    @Transactional(readOnly = true)
    @SneakyThrows
    @Override
    public InputStream getResourceData(TenantId tenantId, TbResourceId resourceId) {
        Blob data = resourceRepository.getDataById(resourceId.getId());
        if (data != null) {
            return data.getBinaryStream();
        } else {
            throw new IllegalArgumentException("Couldn't find resource data");
        }
    }

    @Transactional(readOnly = true)
    @SneakyThrows
    @Override
    public InputStream getResourcePreview(TenantId tenantId, TbResourceId resourceId) {
        Blob preview = resourceRepository.getPreviewById(resourceId.getId());
        if (preview != null) {
            return preview.getBinaryStream();
        } else {
            return getResourceData(tenantId, resourceId);
        }
    }

    @Override
    public long getResourceSize(TenantId tenantId, TbResourceId resourceId) {
        return resourceRepository.getDataSizeById(resourceId.getId());
    }

    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @Override
    public TbResource findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(resourceRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Deprecated
    @Override
    public PageData<TbResource> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAllByTenantId(TenantId.fromUUID(tenantId), pageLink);
    }

    @Override
    public PageData<TbResourceId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(resourceRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink))
                .map(TbResourceId::new));
    }

    @Override
    public TbResourceId getExternalIdByInternal(TbResourceId internalId) {
        return DaoUtil.toEntityId(resourceRepository.getExternalIdByInternal(internalId.getId()), TbResourceId::new);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

}
