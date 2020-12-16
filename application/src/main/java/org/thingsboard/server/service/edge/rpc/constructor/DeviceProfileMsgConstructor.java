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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.edge.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

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
                .setDescription(deviceProfile.getDescription())
                .setDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setTransportType(deviceProfile.getTransportType().name())
                .setProvisionType(deviceProfile.getProvisionType().name())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile.getProfileData())));
        if (deviceProfile.getDefaultRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(deviceProfile.getDefaultRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(deviceProfile.getDefaultRuleChainId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        if (deviceProfile.getProvisionDeviceKey() != null) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
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
