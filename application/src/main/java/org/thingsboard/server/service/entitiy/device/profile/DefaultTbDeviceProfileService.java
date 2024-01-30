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
package org.thingsboard.server.service.entitiy.device.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.utils.AlarmRuleMigrator;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.profile.alarm.rule.DeviceProfileAlarm;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Collections;
import java.util.List;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbDeviceProfileService extends AbstractTbEntityService implements TbDeviceProfileService {

    private final DeviceProfileService deviceProfileService;

    @Override
    @Transactional
    public DeviceProfile save(DeviceProfile deviceProfile, User user) throws Exception {
        ActionType actionType = deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            List<DeviceProfileAlarm> alarms = deviceProfile.getProfileData().getAlarms();
            deviceProfile.getProfileData().setAlarms(null);

            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));

            List<Runnable> alarmRuleCallbacks = Collections.emptyList();

            if (CollectionsUtil.isNotEmpty(alarms)) {
                alarmRuleCallbacks = alarms.stream().map(oldRule -> saveAlarmRule(tenantId, savedDeviceProfile, oldRule, user)).toList();
            }

            autoCommit(user, savedDeviceProfile.getId());
            logEntityActionService.logEntityAction(tenantId, savedDeviceProfile.getId(), savedDeviceProfile, null, actionType, user);
            alarmRuleCallbacks.forEach(Runnable::run);
            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), deviceProfile, actionType, user, e);
            throw e;
        }
    }

    private Runnable saveAlarmRule(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileAlarm oldRule, User user) {
        AlarmRule alarmRule = AlarmRuleMigrator.migrate(tenantId, deviceProfile, oldRule);
        AlarmRule foundAlarmRule = alarmRuleService.findAlarmRuleByName(tenantId, alarmRule.getName());
        if (foundAlarmRule != null) {
            alarmRule.setId(foundAlarmRule.getId());
            alarmRule.setCreatedTime(foundAlarmRule.getCreatedTime());
        }
        ActionType actionType = alarmRule.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        var savedAlarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);
        return () -> logEntityActionService.logEntityAction(tenantId, savedAlarmRule.getId(), savedAlarmRule, null, actionType, user);
    }

    @Override
    public void delete(DeviceProfile deviceProfile, User user) {
        ActionType actionType = ActionType.DELETED;
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);

            logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, null,
                    actionType, user, deviceProfileId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), actionType,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }

    @Override
    public DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException {
        TenantId tenantId = deviceProfile.getTenantId();
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            if (deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, previousDefaultDeviceProfile.getId());
                    logEntityActionService.logEntityAction(tenantId, previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            ActionType.UPDATED, user);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);

                logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, ActionType.UPDATED, user);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), ActionType.UPDATED,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }
}
