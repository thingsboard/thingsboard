/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ResourceCompositeKey;
import org.thingsboard.server.dao.model.sql.ResourceEntity;
import org.thingsboard.server.dao.resource.ResourceDao;

@Slf4j
@Component
public class ResourceDaoImpl implements ResourceDao {

    private final ResourceRepository resourceRepository;

    public ResourceDaoImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    public Resource saveResource(Resource resource) {
        return DaoUtil.getData(resourceRepository.save(new ResourceEntity(resource)));
    }

    @Override
    public Resource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceCompositeKey key = new ResourceCompositeKey();
        key.setTenantId(tenantId.getId());
        key.setResourceType(resourceType.name());
        key.setResourceId(resourceId);

        return DaoUtil.getData(resourceRepository.findById(key));
    }

    @Override
    @Transactional
    public void deleteResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceCompositeKey key = new ResourceCompositeKey();
        key.setTenantId(tenantId.getId());
        key.setResourceType(resourceType.name());
        key.setResourceId(resourceId);

        resourceRepository.deleteById(key);
    }

    @Override
    public PageData<Resource> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void removeAllByTenantId(TenantId tenantId) {
        resourceRepository.removeAllByTenantId(tenantId.getId());
    }
}
