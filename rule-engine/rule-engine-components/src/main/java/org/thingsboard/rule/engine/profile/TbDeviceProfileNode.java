/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.profile;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device profile",
        customRelations = true,
        relationTypes = {"Alarm Created", "Alarm Updated", "Alarm Severity Updated", "Alarm Cleared", "Success", "Failure"},
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Process device messages based on device profile settings",
        nodeDetails = "Create and clear alarms based on alarm rules defined in device profile. Generates ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig"
)
public class TbDeviceProfileNode implements TbNode {

    private RuleEngineDeviceProfileCache cache;
    private Map<DeviceId, DeviceState> deviceStates = new ConcurrentHashMap<>();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        cache = ctx.getDeviceProfileCache();
    }

    /**
     * TODO:
     * 1. Duration in the alarm conditions;
     * 3. Update of the Device attributes (client, server and shared);
     * 4. Dynamic values evaluation;
     */

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityType originatorType = msg.getOriginator().getEntityType();
        if (EntityType.DEVICE.equals(originatorType)) {
            DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
            if (msg.getType().equals("ENTITY_UPDATED")) {
                //TODO: handle if device profile id has changed.
            } else {
                DeviceState deviceState = getOrCreateDeviceState(ctx, deviceId);
                if (deviceState != null) {
                    deviceState.process(ctx, msg);
                } else {
                    ctx.tellFailure(msg, new IllegalStateException("Device profile for device [" + deviceId + "] not found!"));
                }
            }
        } else if (EntityType.DEVICE_PROFILE.equals(originatorType)) {
            if (msg.getType().equals("ENTITY_UPDATED")) {
                DeviceProfile deviceProfile = JacksonUtil.fromString(msg.getData(), DeviceProfile.class);
                for (DeviceState state : deviceStates.values()) {
                    if (deviceProfile.getId().equals(state.getProfileId())) {
                        state.updateProfile(ctx, deviceProfile);
                    }
                }
            }
            ctx.tellSuccess(msg);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private DeviceState getOrCreateDeviceState(TbContext ctx, DeviceId deviceId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceId);
            if (deviceProfile != null) {
                deviceState = new DeviceState(deviceId, new DeviceProfileState(deviceProfile));
                deviceStates.put(deviceId, deviceState);
            }
        }
        return deviceState;
    }

    @Override
    public void destroy() {

    }

}
