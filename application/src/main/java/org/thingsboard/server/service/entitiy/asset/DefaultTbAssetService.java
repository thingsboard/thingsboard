// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.asset;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    private final AssetService assetService;

    @Override
    public Asset save(Asset asset, EntityGroup entityGroup) throws Exception {
        return save(asset, entityGroup, null);
    }

    @Override
    public Asset save(Asset asset, EntityGroup entityGroup, User user) throws Exception {
        return save(asset, entityGroup != null ? Collections.singletonList(entityGroup) : null, user);
    }

    @Override
    public Asset save(Asset asset, List<EntityGroup> entityGroups, User user) throws Exception {
        return save(asset, entityGroups, NameConflictStrategy.DEFAULT, user);
    }

    @Override
    public Asset save(Asset asset, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset, nameConflictStrategy));
            autoCommit(user, savedAsset.getId());
            createOrUpdateGroupEntity(tenantId, savedAsset, entityGroups, actionType, user);
            return savedAsset;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(Asset asset, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            assetService.deleteAsset(tenantId, assetId);
            logEntityActionService.logEntityAction(tenantId, assetId, asset, asset.getCustomerId(), actionType, user, assetId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString());
            throw e;
        }
    }

    @Override
    public void delete(AssetId assetId, User user) {
        Asset asset = assetService.findAssetById(user.getTenantId(), assetId);
        delete(asset, user);
    }
}
