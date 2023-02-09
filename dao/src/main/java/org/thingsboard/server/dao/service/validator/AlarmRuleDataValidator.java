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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
@AllArgsConstructor
public class AlarmRuleDataValidator extends DataValidator<AlarmRule> {

    private final TenantService tenantService;

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
        if (configuration.getSourceEntityFilters() == null) {
            throw new DataValidationException("Alarm rule source filter should be specified!");
        }
        if (CollectionUtils.isEmpty(configuration.getSourceEntityFilters())) {
            throw new DataValidationException("Alarm rule source entity filter should be specified!");
        }
        if (configuration.getAlarmTargetEntity() == null) {
            throw new DataValidationException("Alarm rule target entity should be specified!");
        }
        if (CollectionUtils.isEmpty(configuration.getCreateRules())) {
            throw new DataValidationException("Alarm create rule should be specified!");
        }
    }
}
