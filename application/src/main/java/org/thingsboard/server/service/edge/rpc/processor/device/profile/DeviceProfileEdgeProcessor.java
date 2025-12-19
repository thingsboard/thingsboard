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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class DeviceProfileEdgeProcessor extends BaseDeviceProfileProcessor implements DeviceProfileProcessor {

    @Override
    public ListenableFuture<Void> processDeviceProfileMsgFromEdge(TenantId tenantId, Edge edge, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        log.trace("[{}] executing processDeviceProfileMsgFromEdge [{}] from edge [{}]", tenantId, deviceProfileUpdateMsg, edge.getId());
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (deviceProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process DeviceProfileUpdateMsg from Edge [{}]", tenantId, deviceProfileUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), deviceProfileId);
            pushDeviceProfileCreatedEventToRuleEngine(tenantId, edge, deviceProfileId);
        }
        Boolean deviceProfileNameUpdated = resultPair.getSecond();
        if (deviceProfileNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE, EdgeEventActionType.UPDATED, deviceProfileId, null);
        }
    }

    private void pushDeviceProfileCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DeviceProfileId deviceProfileId) {
        try {
            DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
            String deviceProfileAsString = JacksonUtil.toString(deviceProfile);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, deviceProfileId, null, TbMsgType.ENTITY_CREATED, deviceProfileAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device profile action to rule engine: {}", tenantId, deviceProfileId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(edgeEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg = EdgeMsgConstructorUtils.constructDeviceProfileUpdatedMsg(msgType, deviceProfile);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                DeviceProfileUpdateMsg deviceProfileUpdateMsg = EdgeMsgConstructorUtils.constructDeviceProfileDeleteMsg(deviceProfileId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId) {
        deviceProfile.setDefaultRuleChainId(ruleChainId);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = ruleChainId != null ? ruleChainId.getId() : null;
        deviceProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : null);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultDashboardUUID = deviceProfile.getDefaultDashboardId() != null ? deviceProfile.getDefaultDashboardId().getId() : (dashboardId != null ? dashboardId.getId() : null);
        deviceProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : null);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.DEVICE_PROFILE;
    }

}
