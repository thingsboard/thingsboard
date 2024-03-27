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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityListEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.List;
import java.util.Set;

@Service
@TbCoreComponent
public class AlarmRuleExportService extends BaseEntityExportService<AlarmRuleId, AlarmRule, EntityExportData<AlarmRule>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, AlarmRule alarmRule, EntityExportData<AlarmRule> exportData) {
        List<AlarmRuleEntityFilter> filters = alarmRule.getConfiguration().getSourceEntityFilters();
        for (int i = 0; i < filters.size(); i++) {
            filters.set(i, setSourceEntityId(filters.get(i), ctx));
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ALARM_RULE);
    }

    private AlarmRuleEntityFilter setSourceEntityId(AlarmRuleEntityFilter entityFilter, EntitiesExportCtx<?> ctx) {
        return switch (entityFilter.getType()) {
            case SINGLE_ENTITY -> {
                var single = (AlarmRuleSingleEntityFilter) entityFilter;
                yield new AlarmRuleSingleEntityFilter(getExternalIdOrElseInternal(ctx, single.getEntityId()));
            }
            case DEVICE_TYPE -> {
                var profileType = (AlarmRuleDeviceTypeEntityFilter) entityFilter;
                List<DeviceProfileId> deviceProfileIds =
                        profileType.getDeviceProfileIds().stream().map(id -> getExternalIdOrElseInternal(ctx, id)).toList();
                yield new AlarmRuleDeviceTypeEntityFilter(deviceProfileIds);
            }
            case ASSET_TYPE -> {
                var profileType = (AlarmRuleAssetTypeEntityFilter) entityFilter;
                List<AssetProfileId> assetProfileIds =
                        profileType.getAssetProfileIds().stream().map(id -> getExternalIdOrElseInternal(ctx, id)).toList();
                yield new AlarmRuleAssetTypeEntityFilter(assetProfileIds);
            }
            case ENTITY_LIST -> {
                var listFilter = (AlarmRuleEntityListEntityFilter) entityFilter;
                List<EntityId> entityIds =
                        listFilter.getEntityIds().stream().map(id -> getExternalIdOrElseInternal(ctx, id)).toList();
                yield new AlarmRuleEntityListEntityFilter(listFilter.getEntityType(), entityIds);
            }
            default -> entityFilter;
        };
    }
}
