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
package org.thingsboard.server.dao.sql.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.edqs.fields.TenantFields;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.model.sql.TenantInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
public class JpaTenantDao extends JpaAbstractDao<TenantEntity, Tenant> implements TenantDao {

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    @Override
    protected JpaRepository<TenantEntity, UUID> getRepository() {
        return tenantRepository;
    }

    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(tenantRepository.findTenantInfoById(id));
    }

    @Override
    public PageData<Tenant> findTenants(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantsNextPage(
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TenantInfo> findTenantInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantInfosNextPage(
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, TenantInfoEntity.tenantInfoColumnMap)));
    }

    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(tenantRepository.findTenantsIds(DaoUtil.toPageable(pageLink))).mapData(TenantId::fromUUID);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        return tenantRepository.findTenantIdsByTenantProfileId(tenantProfileId.getId()).stream()
                .map(TenantId::fromUUID)
                .collect(Collectors.toList());
    }

    @Override
    public Tenant findTenantByName(TenantId tenantId, String name) {
        return DaoUtil.getData(tenantRepository.findFirstByTitle(name));
    }

    @Override
    public List<Tenant> findTenantsByIds(UUID tenantId, List<UUID> tenantIds) {
        return DaoUtil.convertDataList(tenantRepository.findTenantsByIdIn(tenantIds));
    }

    @Override
    public List<TenantFields> findNextBatch(UUID id, int batchSize) {
        return tenantRepository.findNextBatch(id, Limit.of(batchSize));
    }

}
