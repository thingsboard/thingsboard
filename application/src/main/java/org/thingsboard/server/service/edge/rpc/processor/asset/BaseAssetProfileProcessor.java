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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class BaseAssetProfileProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        boolean created = false;
        assetCreationLock.lock();
        try {
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
            String assetProfileName = assetProfileUpdateMsg.getName();
            if (assetProfile == null) {
                created = true;
                assetProfile = new AssetProfile();
                assetProfile.setTenantId(tenantId);
                assetProfile.setCreatedTime(Uuids.unixTimestamp(assetProfileId.getId()));
            }
            assetProfile.setName(assetProfileName);
            assetProfile.setDefault(assetProfileUpdateMsg.getDefault());
            assetProfile.setDefaultQueueName(assetProfileUpdateMsg.hasDefaultQueueName() ? assetProfileUpdateMsg.getDefaultQueueName() : null);
            assetProfile.setDescription(assetProfileUpdateMsg.hasDescription() ? assetProfileUpdateMsg.getDescription() : null);
            assetProfile.setImage(assetProfileUpdateMsg.hasImage()
                    ? new String(assetProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);

            UUID defaultRuleChainUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultRuleChainIdMSB(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());
            assetProfile.setDefaultRuleChainId(defaultRuleChainUUID != null ? new RuleChainId(defaultRuleChainUUID) : null);

            UUID defaultDashboardUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultDashboardIdMSB(), assetProfileUpdateMsg.getDefaultDashboardIdLSB());
            assetProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : null);

            assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
            if (created) {
                assetProfile.setId(assetProfileId);
            }
            assetProfileService.saveAssetProfile(assetProfile, false);
        } finally {
            assetCreationLock.unlock();
        }
        return created;
    }
}
