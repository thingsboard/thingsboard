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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityListEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType.ASSET_TYPE;
import static org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType.DEVICE_TYPE;
import static org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType.ENTITY_LIST;
import static org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilterType.SINGLE_ENTITY;

@Component
@AllArgsConstructor
public class AlarmRuleDataValidator extends DataValidator<AlarmRule> {

    private final TenantService tenantService;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;

    private static final int MAX_DEPTH = 5; //TODO: add to the tenant profile

    @Override
    protected void validateDataImpl(TenantId tenantId, AlarmRule alarmRule) {
        if (StringUtils.isEmpty(alarmRule.getName())) {
            throw new DataValidationException("Alarm rule name should be specified!");
        }
        if (StringUtils.isEmpty(alarmRule.getAlarmType())) {
            throw new DataValidationException("Alarm rule type should be specified!");
        }
        if (alarmRule.getTenantId() == null) {
            throw new DataValidationException("Alarm rule should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(alarmRule.getTenantId())) {
                throw new DataValidationException("Alarm rule is referencing to non-existent tenant!");
            }
        }

        AlarmRuleConfiguration configuration = alarmRule.getConfiguration();
        if (configuration == null) {
            throw new DataValidationException("Alarm rule configuration should be specified!");
        }
        if (CollectionUtils.isEmpty(configuration.getSourceEntityFilters())) {
            throw new DataValidationException("Alarm rule source entity filter should be specified!");
        }
        configuration.getSourceEntityFilters().forEach(filter -> validateSourceEntityFilter(tenantId, filter));

        if (CollectionUtils.isEmpty(configuration.getCreateRules())) {
            throw new DataValidationException("Alarm create rule should be specified!");
        }
        configuration.getCreateRules().values().forEach(condition -> {
            validateAlarmConditionFilterDepthAndArguments(condition.getAlarmCondition(), configuration.getArguments(), MAX_DEPTH);
        });
        if (configuration.getClearRule() != null) {
            validateAlarmConditionFilterDepthAndArguments(configuration.getClearRule().getAlarmCondition(), configuration.getArguments(), MAX_DEPTH);
        }
    }

    private void validateSourceEntityFilter(TenantId tenantId, AlarmRuleEntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY ->
                    validateSourceEntityFilter(tenantId, ((AlarmRuleSingleEntityFilter) entityFilter).getEntityId(), SINGLE_ENTITY);
            case DEVICE_TYPE ->
                    validateSourceEntityListFilter(tenantId, ((AlarmRuleDeviceTypeEntityFilter) entityFilter).getDeviceProfileIds(), DEVICE_TYPE);
            case ASSET_TYPE ->
                    validateSourceEntityListFilter(tenantId, ((AlarmRuleAssetTypeEntityFilter) entityFilter).getAssetProfileIds(), ASSET_TYPE);
            case ENTITY_LIST ->
                    validateSourceEntityListFilter(tenantId, ((AlarmRuleEntityListEntityFilter) entityFilter).getEntityIds(), ENTITY_LIST);
        }
    }

    private void validateSourceEntityListFilter(TenantId tenantId, List<? extends EntityId> entityIds, AlarmRuleEntityFilterType entityFilterType) {
        if (CollectionUtils.isEmpty(entityIds)) {
            throw new DataValidationException(String.format("EntityIds should be specified in Alarm Rule %s filter!", entityFilterType));
        }
        entityIds.forEach(entityId -> validateSourceEntityFilter(tenantId, entityId, entityFilterType));
    }

    private void validateSourceEntityFilter(TenantId tenantId, EntityId entityId, AlarmRuleEntityFilterType entityFilterType) {
        if (entityId == null) {
            throw new DataValidationException(String.format("EntityId should be specified in Alarm Rule %s filter!", entityFilterType));
        }

        var entity = switch (entityId.getEntityType()) {
            case DEVICE -> deviceService.findDeviceById(tenantId, (DeviceId) entityId);
            case ASSET -> assetService.findAssetById(tenantId, (AssetId) entityId);
            case DEVICE_PROFILE -> deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) entityId);
            case ASSET_PROFILE -> assetProfileService.findAssetProfileById(tenantId, (AssetProfileId) entityId);
            default ->
                    throw new DataValidationException(String.format("%s entity type does not supported in Alarm Rule %s filter!", entityId.getEntityType(), entityFilterType));
        };

        if (entity == null) {
            throw new DataValidationException(String.format("Can't use non-existent %s in Alarm Rule %s filter! [%s]", entityId.getEntityType(), entityFilterType, entityId));
        }
    }

    private void validateAlarmConditionFilterDepthAndArguments(AlarmCondition alarmCondition, Map<String, AlarmRuleArgument> arguments, int maxDepth) {
        Queue<TbPair<AlarmConditionFilter, Integer>> queue = new LinkedList<>();
        queue.offer(new TbPair<>(alarmCondition.getConditionFilter(), 0));

        while (!queue.isEmpty()) {
            TbPair<AlarmConditionFilter, Integer> pair = queue.poll();
            AlarmConditionFilter currentFilter = pair.getFirst();
            int currentDepth = pair.getSecond();

            if (currentDepth > maxDepth) {
                throw new DataValidationException("Maximum depth of " + maxDepth + " exceeded!");
            }

            if (currentFilter instanceof ComplexAlarmConditionFilter complexFilter) {
                for (AlarmConditionFilter child : complexFilter.getConditions()) {
                    queue.offer(new TbPair<>(child, currentDepth + 1));
                }
            } else {
                var simpleCondition = (SimpleAlarmConditionFilter) currentFilter;

                if (alarmCondition.getSpec() != null && alarmCondition.getSpec().getType() == AlarmConditionSpecType.NO_UPDATE) {
                    checkAndGetArgument(simpleCondition.getLeftArgId(), arguments);
                } else {
                    var leftArg = checkAndGetArgument(simpleCondition.getLeftArgId(), arguments);
                    var rightArg = checkAndGetArgument(simpleCondition.getRightArgId(), arguments);

                    if (leftArg.getValueType() != rightArg.getValueType()) {
                        throw new DataValidationException(String.format("Simple condition arguments have different value types, left: %s, right: %s!", leftArg.getValueType(), rightArg.getValueType()));
                    }
                    if (simpleCondition.getOperation() == null) {
                        throw new DataValidationException("Operation in SimpleConditionFilter should be specified!");
                    }
                    if (!leftArg.getValueType().isAvailable(simpleCondition.getOperation())) {
                        throw new DataValidationException(String.format("Argument value type: %s does not support operation: %s!", leftArg.getValueType(), simpleCondition.getOperation()));
                    }
                }
            }
        }
    }

    private AlarmRuleArgument checkAndGetArgument(String argumentId, Map<String, AlarmRuleArgument> arguments) {
        if (argumentId == null) {
            throw new DataValidationException("Alarm rule argument should be specified!");
        }
        AlarmRuleArgument argument = arguments.get(argumentId);
        if (argument == null) {
            throw new DataValidationException(String.format("Alarm rule argument with id: %s not found!", argumentId));
        }
        return argument;
    }
}
