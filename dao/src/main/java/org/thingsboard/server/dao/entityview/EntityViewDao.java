// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
public interface EntityViewDao extends Dao<EntityView>, ExportableCustomerEntityDao<EntityView, EntityViewId> {

    /**
     * Save or update device object
     *
     * @param entityView the entity-view object
     * @return saved entity-view object
     */
    EntityView save(TenantId tenantId, EntityView entityView);

    /**
     * Find entity views by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity views by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity views by tenantId and entity view name.
     *
     * @param tenantId the tenantId
     * @param name the entity view name
     * @return the optional entity view object
     */
    Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find entity views by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                            UUID customerId,
                                                            PageLink pageLink);

    /**
     * Find entity views by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId,
                                                                   UUID customerId,
                                                                   String type,
                                                                   PageLink pageLink);

    List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * Find entity views by tenantId and entity view Ids.
     *
     * @param tenantId the tenantId
     * @param entityViewIds the entity view Ids
     * @return the list of entity view objects
     */
    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> entityViewIds);

    List<EntityView> findEntityViewsByTenantIdAndIds(UUID tenantId, List<UUID> entityViewIds);

    PageData<EntityView> findEntityViewsByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink);

    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * Find tenants entity view types.
     *
     * @return the list of tenant entity view type objects
     */
    ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId);
}
