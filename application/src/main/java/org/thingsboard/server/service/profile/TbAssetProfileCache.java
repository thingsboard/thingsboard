// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.profile;

import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAssetProfileCache extends RuleEngineAssetProfileCache {

    void evict(TenantId tenantId, AssetProfileId id);

    void evict(TenantId tenantId, AssetId id);

    AssetProfile find(AssetProfileId assetProfileId);

    AssetProfile findOrCreateAssetProfile(TenantId tenantId, String assetType);
}
