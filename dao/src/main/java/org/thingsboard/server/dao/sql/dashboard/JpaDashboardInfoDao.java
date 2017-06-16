/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.annotation.SqlDao;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDashboardInfoDao extends JpaAbstractSearchTextDao<DashboardInfoEntity, DashboardInfo> implements DashboardInfoDao {

    @Autowired
    private DashboardInfoRepository dashboardInfoRepository;

    @Override
    protected Class getEntityClass() {
        return DashboardInfoEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return dashboardInfoRepository;
    }

    @Override
    public List<DashboardInfo> findDashboardsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(dashboardInfoRepository
                .findByTenantId(
                        pageLink.getLimit(),
                        tenantId,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }

    @Override
    public List<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(dashboardInfoRepository
                .findByTenantIdAndCustomerId(
                        pageLink.getLimit(),
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }
}
