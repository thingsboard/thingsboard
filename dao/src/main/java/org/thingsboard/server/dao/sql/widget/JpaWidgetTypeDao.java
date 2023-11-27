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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetCompositeKey;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
@Component
@SqlDao
public class JpaWidgetTypeDao extends JpaAbstractDao<WidgetTypeDetailsEntity, WidgetTypeDetails> implements WidgetTypeDao {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;

    @Autowired
    private WidgetTypeInfoRepository widgetTypeInfoRepository;

    @Autowired
    private WidgetsBundleWidgetRepository widgetsBundleWidgetRepository;

    @Override
    protected Class<WidgetTypeDetailsEntity> getEntityClass() {
        return WidgetTypeDetailsEntity.class;
    }

    @Override
    protected JpaRepository<WidgetTypeDetailsEntity, UUID> getRepository() {
        return widgetTypeRepository;
    }

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeById(widgetTypeId));
    }

    @Override
    public boolean existsByTenantIdAndId(TenantId tenantId, UUID widgetTypeId) {
        return widgetTypeRepository.existsByTenantIdAndId(tenantId.getId(), widgetTypeId);
    }

    @Override
    public PageData<WidgetTypeInfo> findSystemWidgetTypes(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findSystemWidgetTypes(
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findAllTenantWidgetTypesByTenantId(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes,  PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findTenantWidgetTypesByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<WidgetType> findWidgetTypesByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesByWidgetsBundleId(widgetsBundleId));
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesDetailsByWidgetsBundleId(widgetsBundleId));
    }

    @Override
    public PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findWidgetTypesInfosByWidgetsBundleId(
                                widgetsBundleId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<String> findWidgetFqnsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return widgetTypeRepository.findWidgetFqnsByWidgetsBundleId(widgetsBundleId);
    }

    @Override
    public WidgetType findByTenantIdAndFqn(UUID tenantId, String fqn) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeByTenantIdAndFqn(tenantId, fqn));
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesInfosByTenantIdAndResourceId(UUID tenantId, UUID tbResourceId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesInfosByTenantIdAndResourceId(tenantId, tbResourceId));
    }

    @Override
    public List<WidgetTypeId> findWidgetTypeIdsByTenantIdAndFqns(UUID tenantId, List<String> widgetFqns) {
        var idFqnPairs = widgetTypeRepository.findWidgetTypeIdsByTenantIdAndFqns(tenantId, widgetFqns);
        idFqnPairs.sort(Comparator.comparingInt(o -> widgetFqns.indexOf(o.getFqn())));
        return idFqnPairs.stream()
                .map(id -> new WidgetTypeId(id.getId())).collect(Collectors.toList());
    }

    @Override
    public WidgetTypeDetails findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(widgetTypeRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<WidgetTypeDetails> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetTypeRepository
                        .findTenantWidgetTypeDetailsByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public WidgetTypeId getExternalIdByInternal(WidgetTypeId internalId) {
        return Optional.ofNullable(widgetTypeRepository.getExternalIdById(internalId.getId()))
                .map(WidgetTypeId::new).orElse(null);
    }

    @Override
    public List<WidgetsBundleWidget> findWidgetsBundleWidgetsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetsBundleWidgetRepository.findAllByWidgetsBundleId(widgetsBundleId));
    }

    @Override
    public void saveWidgetsBundleWidget(WidgetsBundleWidget widgetsBundleWidget) {
        widgetsBundleWidgetRepository.save(new WidgetsBundleWidgetEntity(widgetsBundleWidget));
    }

    @Override
    public void removeWidgetTypeFromWidgetsBundle(UUID widgetsBundleId, UUID widgetTypeId) {
        widgetsBundleWidgetRepository.deleteById(new WidgetsBundleWidgetCompositeKey(widgetsBundleId, widgetTypeId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }


}
