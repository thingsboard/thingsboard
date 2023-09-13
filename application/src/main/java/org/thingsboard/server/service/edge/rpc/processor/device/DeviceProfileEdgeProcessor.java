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
package org.thingsboard.server.service.edge.rpc.processor.device;

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
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class DeviceProfileEdgeProcessor extends BaseDeviceProfileProcessor {

    public ListenableFuture<Void> processDeviceProfileMsgFromEdge(TenantId tenantId, Edge edge, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        log.trace("[{}] executing processDeviceProfileMsgFromEdge [{}] from edge [{}]", tenantId, deviceProfileUpdateMsg, edge.getName());
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (deviceProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process DeviceProfileUpdateMsg from Edge [{}]", tenantId, deviceProfileUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getSync().remove();
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
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
            String deviceProfileAsString = JacksonUtil.toString(deviceProfile);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, deviceProfileId, null, TbMsgType.ENTITY_CREATED, deviceProfileAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device profile action to rule engine: {}", tenantId, deviceProfileId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    public DownlinkMsg convertDeviceProfileEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    deviceProfile = checkIfDeviceProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, deviceProfile, edgeVersion);
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                            deviceProfileMsgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                        deviceProfileMsgConstructor.constructDeviceProfileDeleteMsg(deviceProfileId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        // do nothing on cloud
    }

    @Override
    protected void setDefaultEdgeRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB(), deviceProfileUpdateMsg.getDefaultRuleChainIdLSB());
        deviceProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : null);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultDashboardIdMSB(), deviceProfileUpdateMsg.getDefaultDashboardIdLSB());
        deviceProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : null);
    }
}
