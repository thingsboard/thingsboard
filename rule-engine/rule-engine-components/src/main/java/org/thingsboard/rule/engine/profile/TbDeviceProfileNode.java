/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device profile",
        customRelations = true,
        relationTypes = {"Alarm Created", "Alarm Updated", "Alarm Severity Updated", "Alarm Cleared", "Success", "Failure"},
        version = 1,
        configClazz = TbDeviceProfileNodeConfiguration.class,
        nodeDescription = "Process device messages based on device profile settings",
        nodeDetails = "Create and clear alarms based on alarm rules defined in device profile. The output relation type is either " +
                "'Alarm Created', 'Alarm Updated', 'Alarm Severity Updated' and 'Alarm Cleared' or simply 'Success' if no alarms were affected.",
        configDirective = "tbActionNodeDeviceProfileConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/device-profile/"
)
public class TbDeviceProfileNode implements TbNode {

    private TbDeviceProfileNodeConfiguration config;
    private RuleEngineDeviceProfileCache cache;
    private TbContext ctx;
    private final Map<DeviceId, DeviceState> deviceStates = new ConcurrentHashMap<>();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeviceProfileNodeConfiguration.class);
        this.cache = ctx.getDeviceProfileCache();
        this.ctx = ctx;
        scheduleAlarmHarvesting(ctx, null);
        ctx.addDeviceProfileListeners(this::onProfileUpdate, this::onDeviceUpdate);
        initAlarmRuleState(false);
    }

    private void initAlarmRuleState(boolean printNewlyAddedDeviceStates) {
        if (config.isFetchAlarmRulesStateOnStart()) {
            log.info("[{}] Fetching alarm rule state", ctx.getSelfId());
            int fetchCount = 0;
            PageLink pageLink = new PageLink(1024);
            while (true) {
                PageData<RuleNodeState> states = ctx.findRuleNodeStates(pageLink);
                if (!states.getData().isEmpty()) {
                    for (RuleNodeState rns : states.getData()) {
                        fetchCount++;
                        if (rns.getEntityId().getEntityType().equals(EntityType.DEVICE) && ctx.isLocalEntity(rns.getEntityId())) {
                            getOrCreateDeviceState(ctx, new DeviceId(rns.getEntityId().getId()), rns, printNewlyAddedDeviceStates);
                        }
                    }
                }
                if (!states.hasNext()) {
                    break;
                } else {
                    pageLink = pageLink.nextPageLink();
                }
            }
            log.info("[{}] Fetched alarm rule state for {} entities", ctx.getSelfId(), fetchCount);
        }
        if (!config.isPersistAlarmRulesState() && ctx.isLocalEntity(ctx.getSelfId())) {
            log.debug("[{}] Going to cleanup rule node states", ctx.getSelfId());
            ctx.clearRuleNodeStates();
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityType originatorType = msg.getOriginator().getEntityType();
        if (msg.isTypeOf(TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG)) {
            scheduleAlarmHarvesting(ctx, msg);
            harvestAlarms(ctx, System.currentTimeMillis());
        } else if (msg.isTypeOf(TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG)) {
            updateProfile(ctx, new DeviceProfileId(UUID.fromString(msg.getData())));
        } else if (msg.isTypeOf(TbMsgType.DEVICE_UPDATE_SELF_MSG)) {
            JsonNode data = JacksonUtil.toJsonNode(msg.getData());
            DeviceId deviceId = new DeviceId(UUID.fromString(data.get("deviceId").asText()));
            if (data.has("profileId")) {
                invalidateDeviceProfileCache(deviceId, new DeviceProfileId(UUID.fromString(data.get("deviceProfileId").asText())));
            } else {
                removeDeviceState(deviceId);
            }
        } else {
            if (EntityType.DEVICE.equals(originatorType)) {
                DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
                if (msg.isTypeOf(TbMsgType.ENTITY_UPDATED)) {
                    invalidateDeviceProfileCache(deviceId, msg.getData());
                    ctx.tellSuccess(msg);
                } else if (msg.isTypeOf(TbMsgType.ENTITY_DELETED)) {
                    removeDeviceState(deviceId);
                    ctx.tellSuccess(msg);
                } else {
                    DeviceState deviceState = getOrCreateDeviceState(ctx, deviceId, null, false);
                    if (deviceState != null) {
                        deviceState.process(ctx, msg);
                    } else {
                        log.info("Device was not found! Most probably device [{}] has been removed from the database. Acknowledging msg.", deviceId);
                        ctx.ack(msg);
                    }
                }
            } else {
                ctx.tellSuccess(msg);
            }
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        // Cleanup the cache for all entities that are no longer assigned to current server partitions
        deviceStates.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
        initAlarmRuleState(true);
    }

    @Override
    public void destroy() {
        ctx.removeListeners();
        deviceStates.clear();
    }

    private DeviceState getOrCreateDeviceState(TbContext ctx, DeviceId deviceId, RuleNodeState rns, boolean printNewlyAddedDeviceStates) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceId);
            if (deviceProfile != null) {
                deviceState = new DeviceState(ctx, config, deviceId, new ProfileState(deviceProfile), rns);
                deviceStates.put(deviceId, deviceState);
                if (printNewlyAddedDeviceStates) {
                    log.info("[{}][{}] Device [{}] was added during PartitionChangeMsg", ctx.getTenantId(), ctx.getSelfId(), deviceId);
                }
            }
        }
        return deviceState;
    }

    protected void scheduleAlarmHarvesting(TbContext ctx, TbMsg msg) {
        CustomerId customerId = msg != null ? msg.getCustomerId() : null;
        TbMsg periodicCheck = TbMsg.newMsg()
                .type(TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG)
                .originator(ctx.getTenantId())
                .customerId(customerId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        ctx.tellSelf(periodicCheck, TimeUnit.MINUTES.toMillis(1));
    }

    protected void harvestAlarms(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        for (DeviceState state : deviceStates.values()) {
            state.harvestAlarms(ctx, ts);
        }
    }

    protected void updateProfile(TbContext ctx, DeviceProfileId deviceProfileId) throws ExecutionException, InterruptedException {
        DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceProfileId);
        if (deviceProfile != null) {
            log.debug("[{}] Received device profile update notification: {}", ctx.getSelfId(), deviceProfile);
            for (DeviceState state : deviceStates.values()) {
                if (deviceProfile.getId().equals(state.getProfileId())) {
                    state.updateProfile(ctx, deviceProfile);
                }
            }
        } else {
            log.debug("[{}] Received stale profile update notification: [{}]", ctx.getSelfId(), deviceProfileId);
        }
    }

    protected void onProfileUpdate(DeviceProfile profile) {
        ctx.tellSelf(TbMsg.newMsg()
                .type(TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG)
                .originator(ctx.getTenantId())
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(profile.getId().getId().toString())
                .build(), 0L);
    }

    private void onDeviceUpdate(DeviceId deviceId, DeviceProfile deviceProfile) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("deviceId", deviceId.getId().toString());
        if (deviceProfile != null) {
            msgData.put("deviceProfileId", deviceProfile.getId().getId().toString());
        }
        ctx.tellSelf(TbMsg.newMsg()
                .type(TbMsgType.DEVICE_UPDATE_SELF_MSG)
                .originator(ctx.getTenantId())
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(JacksonUtil.toString(msgData))
                .build(), 0L);
    }

    protected void invalidateDeviceProfileCache(DeviceId deviceId, String deviceJson) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            DeviceProfileId currentProfileId = deviceState.getProfileId();
            try {
                Device device = JacksonUtil.fromString(deviceJson, Device.class);
                if (!currentProfileId.equals(device.getDeviceProfileId())) {
                    removeDeviceState(deviceId);
                }
            } catch (IllegalArgumentException e) {
                log.debug("[{}] Received device update notification with non-device msg body: [{}]", ctx.getSelfId(), deviceId, e);
            }
        }
    }

    protected void invalidateDeviceProfileCache(DeviceId deviceId, DeviceProfileId deviceProfileId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            if (!deviceState.getProfileId().equals(deviceProfileId)) {
                removeDeviceState(deviceId);
            }
        }
    }

    private void removeDeviceState(DeviceId deviceId) {
        DeviceState state = deviceStates.remove(deviceId);
        if (config.isPersistAlarmRulesState() && (state != null || !config.isFetchAlarmRulesStateOnStart())) {
            ctx.removeRuleNodeStateForEntity(deviceId);
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                String persistAlarmRulesState = "persistAlarmRulesState";
                String fetchAlarmRulesStateOnStart = "fetchAlarmRulesStateOnStart";
                if (oldConfiguration.has(persistAlarmRulesState)) {
                    if (!oldConfiguration.get(persistAlarmRulesState).asBoolean()) {
                        hasChanges = true;
                        ((ObjectNode) oldConfiguration).put(fetchAlarmRulesStateOnStart, false);
                    }
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
