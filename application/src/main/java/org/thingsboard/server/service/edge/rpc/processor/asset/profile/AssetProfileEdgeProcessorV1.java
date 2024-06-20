/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.asset.profile;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@TbCoreComponent
public class AssetProfileEdgeProcessorV1 extends AssetProfileEdgeProcessor {

    @Override
    protected AssetProfile constructAssetProfileFromUpdateMsg(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(assetProfileUpdateMsg.getName());
        assetProfile.setCreatedTime(Uuids.unixTimestamp(assetProfileId.getId()));
        assetProfile.setDefault(assetProfileUpdateMsg.getDefault());
        assetProfile.setDefaultQueueName(assetProfileUpdateMsg.hasDefaultQueueName() ? assetProfileUpdateMsg.getDefaultQueueName() : null);
        assetProfile.setDescription(assetProfileUpdateMsg.hasDescription() ? assetProfileUpdateMsg.getDescription() : null);
        assetProfile.setImage(assetProfileUpdateMsg.hasImage()
                ? new String(assetProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
        return assetProfile;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId) {
        assetProfile.setDefaultRuleChainId(ruleChainId);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultRuleChainIdMSB(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());
        assetProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : ruleChainId);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultDashboardIdMSB(), assetProfileUpdateMsg.getDefaultDashboardIdLSB());
        assetProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : dashboardId);
    }
}
