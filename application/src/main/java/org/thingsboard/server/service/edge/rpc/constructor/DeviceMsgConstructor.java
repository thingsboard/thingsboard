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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RpcRequestMsg;
import org.thingsboard.server.gen.edge.v1.RpcResponseMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@TbCoreComponent
public class DeviceMsgConstructor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device, String conflictName) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        if (device.getCustomerId() != null) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }
        if (device.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits());
            builder.setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());
        }
        if (device.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));
        }
        if (device.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(device.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(device.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (conflictName != null) {
            builder.setConflictName(conflictName);
        }
        if (device.getDeviceData() != null) {
            builder.setDeviceDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(device.getDeviceData())));
        }
        return builder.build();
    }

    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        DeviceCredentialsUpdateMsg.Builder builder = DeviceCredentialsUpdateMsg.newBuilder()
                .setDeviceIdMSB(deviceCredentials.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceCredentials.getDeviceId().getId().getLeastSignificantBits());
        if (deviceCredentials.getCredentialsType() != null) {
            builder.setCredentialsType(deviceCredentials.getCredentialsType().name())
                    .setCredentialsId(deviceCredentials.getCredentialsId());
        }
        if (deviceCredentials.getCredentialsValue() != null) {
            builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
        }
        return builder.build();
    }

    public DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId) {
        return DeviceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceId.getId().getMostSignificantBits())
                .setIdLSB(deviceId.getId().getLeastSignificantBits()).build();
    }

    public DeviceRpcCallMsg constructDeviceRpcCallMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = constructDeviceRpcMsg(deviceId, body);
        if (body.has("error") || body.has("response")) {
            RpcResponseMsg.Builder responseBuilder = RpcResponseMsg.newBuilder();
            if (body.has("error")) {
                responseBuilder.setError(body.get("error").asText());
            } else {
                responseBuilder.setResponse(body.get("response").asText());
            }
            builder.setResponseMsg(responseBuilder.build());
        } else {
            RpcRequestMsg.Builder requestBuilder = RpcRequestMsg.newBuilder();
            requestBuilder.setMethod(body.get("method").asText());
            requestBuilder.setParams(body.get("params").asText());
            builder.setRequestMsg(requestBuilder.build());
        }
        return builder.build();
    }

    private DeviceRpcCallMsg.Builder constructDeviceRpcMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = DeviceRpcCallMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getLeastSignificantBits())
                .setRequestId(body.get("requestId").asInt());
        if (body.get("oneway") != null) {
            builder.setOneway(body.get("oneway").asBoolean());
        }
        if (body.get("requestUUID") != null) {
            UUID requestUUID = UUID.fromString(body.get("requestUUID").asText());
            builder.setRequestUuidMSB(requestUUID.getMostSignificantBits())
                    .setRequestUuidLSB(requestUUID.getLeastSignificantBits());
        }
        if (body.get("expirationTime") != null) {
            builder.setExpirationTime(body.get("expirationTime").asLong());
        }
        if (body.get("persisted") != null) {
            builder.setPersisted(body.get("persisted").asBoolean());
        }
        if (body.get("retries") != null) {
            builder.setRetries(body.get("retries").asInt());
        }
        if (body.get("additionalInfo") != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(body.get("additionalInfo")));
        }
        if (body.get("serviceId") != null) {
            builder.setServiceId(body.get("serviceId").asText());
        }
        if (body.get("sessionId") != null) {
            builder.setSessionId(body.get("sessionId").asText());
        }
        return builder;
    }
}
