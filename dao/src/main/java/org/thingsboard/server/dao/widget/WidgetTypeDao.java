// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.widget;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeFilter;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.ImageContainerDao;
import org.thingsboard.server.dao.ResourceContainerDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface WidgetTypeDao.
 */
public interface WidgetTypeDao extends Dao<WidgetTypeDetails>, ExportableEntityDao<WidgetTypeId, WidgetTypeDetails>, ImageContainerDao<WidgetTypeInfo>, ResourceContainerDao<WidgetTypeInfo> {

    /**
     * Save or update widget type object
     *
     * @param widgetTypeDetails the widget type details object
     * @return saved widget type object
     */
    WidgetTypeDetails save(TenantId tenantId, WidgetTypeDetails widgetTypeDetails);

    /**
     * Find widget type by tenantId and widgetTypeId.
     *
     * @param tenantId the tenantId
     * @param widgetTypeId the widget type id
     * @return the widget type object
     */
    WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId);

    boolean existsByTenantIdAndId(TenantId tenantId, UUID widgetTypeId);

    WidgetTypeInfo findWidgetTypeInfoById(TenantId tenantId, UUID widgetTypeId);

    PageData<WidgetTypeInfo> findSystemWidgetTypes(WidgetTypeFilter widgetTypeFilter, PageLink pageLink);

    PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantId(WidgetTypeFilter widgetTypeFilter, PageLink pageLink);

    PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantId(WidgetTypeFilter widgetTypeFilter, PageLink pageLink);

    /**
     * Find widget types by widgetsBundleId.
     *
     * @param tenantId the tenantId
     * @param widgetsBundleId the widgets bundle id
     * @return the list of widget types objects
     */
    List<WidgetType> findWidgetTypesByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * Find widget types details by widgetsBundleId.
     *
     * @param tenantId the tenantId
     * @param widgetsBundleId the widgets bundle id
     * @return the list of widget types details objects
     */
    List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink);

    List<String> findWidgetFqnsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    /**
     * Find widget type by tenantId and FQN.
     *
     * @param tenantId the tenantId
     * @param fqn the FQN
     * @return the widget type object
     */
    WidgetType findByTenantIdAndFqn(UUID tenantId, String fqn);

    WidgetTypeDetails findDetailsByTenantIdAndFqn(UUID tenantId, String fqn);

    List<WidgetTypeId> findWidgetTypeIdsByTenantIdAndFqns(UUID tenantId, List<String> widgetFqns);

    List<WidgetsBundleWidget> findWidgetsBundleWidgetsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId);

    void saveWidgetsBundleWidget(WidgetsBundleWidget widgetsBundleWidget);

    void removeWidgetTypeFromWidgetsBundle(UUID widgetsBundleId, UUID widgetTypeId);

    PageData<WidgetTypeId> findAllWidgetTypesIds(PageLink pageLink);

}
