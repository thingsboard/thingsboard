// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.asset;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.List;

public interface TbAssetService {

    Asset save(Asset asset, EntityGroup entityGroup) throws Exception;

    Asset save(Asset asset, EntityGroup entityGroup, User user) throws Exception;

    Asset save(Asset asset, List<EntityGroup> entityGroups, User user) throws Exception;

    Asset save(Asset asset, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws Exception;

    void delete(Asset asset, User user);

    void delete(AssetId assetId, User user);
}
