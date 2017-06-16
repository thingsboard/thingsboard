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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.annotation.SqlDao;
import org.thingsboard.server.dao.model.sql.WidgetsBundleEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
@Component
@SqlDao
public class JpaWidgetsBundleDao extends JpaAbstractSearchTextDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    @Autowired
    private WidgetsBundleRepository widgetsBundleRepository;

    @Override
    protected Class<WidgetsBundleEntity> getEntityClass() {
        return WidgetsBundleEntity.class;
    }

    @Override
    protected CrudRepository<WidgetsBundleEntity, UUID> getCrudRepository() {
        return widgetsBundleRepository;
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        return DaoUtil.getData(widgetsBundleRepository.findWidgetsBundleByTenantIdAndAlias(tenantId, alias));
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findSystemWidgetsBundles(
                                pageLink.getLimit(),
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }

    @Override
    public List<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findTenantWidgetsBundlesByTenantId(
                                pageLink.getLimit(),
                                tenantId,
                                pageLink.getTextSearch(),
                                pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findAllTenantWidgetsBundlesByTenantId(
                                pageLink.getLimit(),
                                tenantId,
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }
}
