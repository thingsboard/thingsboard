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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
@Component
public class JpaWidgetTypeDao extends JpaAbstractDao<WidgetTypeDetailsEntity, WidgetTypeDetails> implements WidgetTypeDao {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;

    @Override
    protected Class<WidgetTypeDetailsEntity> getEntityClass() {
        return WidgetTypeDetailsEntity.class;
    }

    @Override
    protected CrudRepository<WidgetTypeDetailsEntity, UUID> getCrudRepository() {
        return widgetTypeRepository;
    }

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeById(widgetTypeId));
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId, bundleAlias));
    }

    @Override
    public WidgetType findByTenantIdBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeByTenantIdAndBundleAliasAndAlias(tenantId, bundleAlias, alias));
    }
}
