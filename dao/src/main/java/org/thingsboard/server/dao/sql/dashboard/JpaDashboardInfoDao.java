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
package org.thingsboard.server.dao.sql.dashboard;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaDashboardInfoDao extends JpaAbstractSearchTextDao<DashboardInfoEntity, DashboardInfo> implements DashboardInfoDao {

    @Autowired
    private DashboardInfoRepository dashboardInfoRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class getEntityClass() {
        return DashboardInfoEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return dashboardInfoRepository;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndCustomerId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        UUIDConverter.fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<PageData<DashboardInfo>> findDashboardsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, TimePageLink pageLink) {
        log.debug("Try to find dashboards by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<PageData<EntityRelation>> relations =
                relationDao.findRelations(new TenantId(tenantId), new EdgeId(edgeId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EntityType.DASHBOARD, pageLink);
        return Futures.transformAsync(relations, relationsData -> {
            if (relationsData != null && relationsData.getData() != null && !relationsData.getData().isEmpty()) {
                List<ListenableFuture<DashboardInfo>> dashboardFutures = new ArrayList<>(relationsData.getData().size());
                for (EntityRelation relation : relationsData.getData()) {
                    dashboardFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
                }
                return Futures.transform(Futures.successfulAsList(dashboardFutures),
                        dashboards -> new PageData<>(dashboards, relationsData.getTotalPages(), relationsData.getTotalElements(),
                                relationsData.hasNext()), MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(new PageData<>());
            }
        }, MoreExecutors.directExecutor());
    }
}
