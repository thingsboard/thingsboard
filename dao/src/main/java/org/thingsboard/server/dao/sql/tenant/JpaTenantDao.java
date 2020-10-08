/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.model.sql.TenantInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.Objects;
import java.util.UUID;


/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Component
public class JpaTenantDao extends JpaAbstractSearchTextDao<TenantEntity, Tenant> implements TenantDao {

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    @Override
    protected CrudRepository<TenantEntity, UUID> getCrudRepository() {
        return tenantRepository;
    }

    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(tenantRepository.findTenantInfoById(id));
    }

    @Override
    public PageData<Tenant> findTenantsByRegion(TenantId tenantId, String region, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findByRegionNextPage(
                        region,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TenantInfo> findTenantInfosByRegion(TenantId tenantId, String region, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantInfoByRegionNextPage(
                        region,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, TenantInfoEntity.tenantInfoColumnMap)));
    }
}
