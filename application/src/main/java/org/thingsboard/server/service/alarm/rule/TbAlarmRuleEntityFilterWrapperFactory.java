/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.alarm.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class TbAlarmRuleEntityFilterWrapperFactory {
    private final RuleEngineDeviceProfileCache deviceProfileCache;
    private final RuleEngineAssetProfileCache assetProfileCache;

    public AlarmRuleEntityFilter wrap(TenantId tenantId, AlarmRuleEntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case DEVICE_TYPE:
                return wrap(tenantId, (AlarmRuleDeviceTypeEntityFilter) entityFilter);
            case ASSET_TYPE:
                return wrap(tenantId, (AlarmRuleAssetTypeEntityFilter) entityFilter);
        }
        return entityFilter;
    }

    private AlarmRuleEntityFilter wrap(TenantId tenantId, AlarmRuleDeviceTypeEntityFilter entityFilter) {
        return new AlarmRuleEntityFilter() {
            @Override
            public AlarmRuleEntityFilterType getType() {
                return entityFilter.getType();
            }

            @Override
            public boolean isEntityMatches(EntityId entityId) {
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, (DeviceId) entityId);
                    return deviceProfile != null && entityFilter.isEntityMatches(deviceProfile.getId());
                }
                return false;
            }
        };
    }

    private AlarmRuleEntityFilter wrap(TenantId tenantId, AlarmRuleAssetTypeEntityFilter entityFilter) {
        return new AlarmRuleEntityFilter() {
            @Override
            public AlarmRuleEntityFilterType getType() {
                return entityFilter.getType();
            }

            @Override
            public boolean isEntityMatches(EntityId entityId) {
                if (entityId.getEntityType() == EntityType.ASSET) {
                    AssetProfile assetProfile = assetProfileCache.get(tenantId, (AssetId) entityId);
                    return assetProfile != null && entityFilter.isEntityMatches(assetProfile.getId());
                }
                return false;
            }
        };
    }
}
