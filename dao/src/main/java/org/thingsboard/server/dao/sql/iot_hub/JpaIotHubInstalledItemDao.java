/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.iot_hub;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemDao;
import org.thingsboard.server.dao.model.sql.IotHubInstalledItemEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaIotHubInstalledItemDao extends JpaAbstractDao<IotHubInstalledItemEntity, IotHubInstalledItem> implements IotHubInstalledItemDao {

    private final IotHubInstalledItemRepository repository;

    @Override
    public PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(
                tenantId.getId(),
                itemTypes == null || itemTypes.isEmpty() ? null : itemTypes,
                itemId,
                StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null),
                toPageRequest(pageLink))
        );
    }

    @Override
    public List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId) {
        return repository.findInstalledItemIdsByTenantId(tenantId.getId());
    }

    @Override
    public long countByTenantId(TenantId tenantId, String itemType) {
        return repository.countByTenantId(tenantId.getId(), itemType);
    }

    @Override
    public Map<UUID, Long> findInstalledItemCounts(TenantId tenantId, String itemType) {
        List<Object[]> results = repository.findInstalledItemCounts(tenantId.getId(), itemType);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : results) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        repository.deleteByTenantId(tenantId.getId());
    }

    private static PageRequest toPageRequest(PageLink pageLink) {
        Sort sort;
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder == null) {
            sort = Sort.by(Sort.Direction.DESC, "createdTime");
        } else {
            sort = Sort.by(
                    Sort.Direction.fromString(sortOrder.getDirection().name()),
                    sortOrder.getProperty()
            ).and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), sort);
    }

    @Override
    protected Class<IotHubInstalledItemEntity> getEntityClass() {
        return IotHubInstalledItemEntity.class;
    }

    @Override
    protected JpaRepository<IotHubInstalledItemEntity, UUID> getRepository() {
        return repository;
    }

}
