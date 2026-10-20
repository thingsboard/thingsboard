// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.blob;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbBlobService extends AbstractTbEntityService implements TbBlobService {

    private final BlobEntityService blobEntityService;

    @Override
    public void delete(BlobEntityInfo blobEntityInfo, User user) {
        TenantId tenantId = blobEntityInfo.getTenantId();
        BlobEntityId  blobEntityId = blobEntityInfo.getId();
        try {
            blobEntityService.deleteBlobEntity(tenantId, blobEntityId);

            logEntityActionService.logEntityAction(tenantId, blobEntityId, blobEntityInfo,
                    blobEntityInfo.getCustomerId(), ActionType.DELETED, user, blobEntityId.getId().toString());

        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.BLOB_ENTITY),
                    ActionType.DELETED, user, e, blobEntityId.getId().toString());
            throw e;
        }
    }
}
