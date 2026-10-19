// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface BlobEntityInfoDao.
 *
 */
public interface BlobEntityInfoDao extends Dao<BlobEntityInfo> {

    BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(UUID tenantId, UUID blobEntityId);

    /**
     * Find blob entities by tenantId.
     *
     * @param tenantId the tenantId
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(UUID tenantId, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and type.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(UUID tenantId, String type, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and customerId.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId, customerId and type.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and blob entity Ids.
     *
     * @param tenantId the tenantId
     * @param blobEntityIds the blob entity Ids
     * @return the list of blob entity objects
     */
    ListenableFuture<List<BlobEntityInfo>> findBlobEntitiesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> blobEntityIds);

}
