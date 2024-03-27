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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityListEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AlarmRuleImportService extends BaseEntityImportService<AlarmRuleId, AlarmRule, EntityExportData<AlarmRule>> {

    private final AlarmRuleService alarmRuleService;

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM_RULE;
    }

    @Override
    protected void setOwner(TenantId tenantId, AlarmRule alarmRule, IdProvider idProvider) {
        alarmRule.setTenantId(tenantId);
    }

    @Override
    protected AlarmRule prepare(EntitiesImportCtx ctx, AlarmRule alarmRule, AlarmRule oldEntity, EntityExportData<AlarmRule> exportData, IdProvider idProvider) {
        List<AlarmRuleEntityFilter> filters = alarmRule.getConfiguration().getSourceEntityFilters();
        for (int i = 0; i < filters.size(); i++) {
            filters.set(i, setSourceEntityId(filters.get(i), idProvider));
        }
        return alarmRule;
    }

    @Override
    protected AlarmRule deepCopy(AlarmRule alarmRule) {
        return new AlarmRule(alarmRule);
    }

    @Override
    protected AlarmRule saveOrUpdate(EntitiesImportCtx ctx, AlarmRule alarmRule, EntityExportData<AlarmRule> exportData, IdProvider idProvider) {
        return alarmRuleService.saveAlarmRule(ctx.getTenantId(), alarmRule);
    }

    @Override
    protected void onEntitySaved(User user, AlarmRule savedAlarmRule, AlarmRule oldEntity) throws ThingsboardException {
        logEntityActionService.logEntityAction(savedAlarmRule.getTenantId(), savedAlarmRule.getId(), savedAlarmRule, null, ActionType.ADDED, user);
    }

    private AlarmRuleEntityFilter setSourceEntityId(AlarmRuleEntityFilter entityFilter, IdProvider idProvider) {
        return switch (entityFilter.getType()) {
            case SINGLE_ENTITY -> {
                var single = (AlarmRuleSingleEntityFilter) entityFilter;
                yield new AlarmRuleSingleEntityFilter(idProvider.getInternalId(single.getEntityId(), true));
            }
            case DEVICE_TYPE -> {
                var profileType = (AlarmRuleDeviceTypeEntityFilter) entityFilter;
                List<DeviceProfileId> deviceProfileIds =
                        profileType.getDeviceProfileIds().stream().map(id -> idProvider.getInternalId(id, true)).toList();
                yield new AlarmRuleDeviceTypeEntityFilter(deviceProfileIds);
            }
            case ASSET_TYPE -> {
                var profileType = (AlarmRuleAssetTypeEntityFilter) entityFilter;
                List<AssetProfileId> assetProfileIds =
                        profileType.getAssetProfileIds().stream().map(id -> idProvider.getInternalId(id, true)).toList();
                yield new AlarmRuleAssetTypeEntityFilter(assetProfileIds);
            }
            case ENTITY_LIST -> {
                var listFilter = (AlarmRuleEntityListEntityFilter) entityFilter;
                List<EntityId> entityIds = listFilter.getEntityIds();
                for (int i = 0; i < entityIds.size(); i++) {
                    entityIds.set(i, idProvider.getInternalId(entityIds.get(i), true));
                }
                yield entityFilter;
            }
            default -> entityFilter;
        };
    }
}
