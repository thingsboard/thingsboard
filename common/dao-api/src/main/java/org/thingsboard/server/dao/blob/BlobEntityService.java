// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface BlobEntityService extends EntityDaoService {

    BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    ListenableFuture<BlobEntityInfo> findBlobEntityInfoByIdAsync(TenantId tenantId, BlobEntityId blobEntityId);

    ListenableFuture<List<BlobEntityInfo>> findBlobEntityInfoByIdsAsync(TenantId tenantId, List<BlobEntityId> blobEntityIds);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(TenantId tenantId, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(TenantId tenantId, String type, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TimePageLink pageLink);

    BlobEntity saveBlobEntity(BlobEntity blobEntity);

    void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId);

    void deleteBlobEntitiesByTenantId(TenantId tenantId);

    void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

}
