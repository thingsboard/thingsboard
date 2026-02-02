/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
