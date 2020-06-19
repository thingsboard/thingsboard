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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

@Component
@Slf4j
public class DeviceUpdateMsgConstructor {

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        if (msgType.equals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE) ||
                msgType.equals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE) ||
                msgType.equals(UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE)) {
            DeviceCredentials deviceCredentials
                    = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
            if (deviceCredentials != null) {
                if (deviceCredentials.getCredentialsType() != null) {
                    builder.setCredentialsType(deviceCredentials.getCredentialsType().name())
                            .setCredentialsId(deviceCredentials.getCredentialsId());
                }
                if (deviceCredentials.getCredentialsValue() != null) {
                    builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
                }
            }
        }
        return builder.build();
    }

    public DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId) {
        return DeviceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceId.getId().getMostSignificantBits())
                .setIdLSB(deviceId.getId().getLeastSignificantBits()).build();
    }
}
