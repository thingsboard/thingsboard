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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
public class DeviceProfileMsgConstructor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        DeviceProfileUpdateMsg.Builder builder = DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits())
                .setName(deviceProfile.getName())
                .setDefault(deviceProfile.isDefaultProfile())
                .setType(deviceProfile.getType().name())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile.getProfileData())));
        if (deviceProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        if (deviceProfile.getDescription() != null) {
            builder.setDescription(deviceProfile.getDescription());
        }
        if (deviceProfile.getTransportType() != null) {
            builder.setTransportType(deviceProfile.getTransportType().name());
        }
        if (deviceProfile.getProvisionType() != null) {
            builder.setProvisionType(deviceProfile.getProvisionType().name());
        }
        if (deviceProfile.getProvisionDeviceKey() != null) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
        }
        if (deviceProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(deviceProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (deviceProfile.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(deviceProfile.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(deviceProfile.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getSoftwareId() != null) {
            builder.setSoftwareIdMSB(deviceProfile.getSoftwareId().getId().getMostSignificantBits())
                    .setSoftwareIdLSB(deviceProfile.getSoftwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(deviceProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(deviceProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public DeviceProfileUpdateMsg constructDeviceProfileDeleteMsg(DeviceProfileId deviceProfileId) {
        return DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceProfileId.getId().getMostSignificantBits())
                .setIdLSB(deviceProfileId.getId().getLeastSignificantBits()).build();
    }

}
