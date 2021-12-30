/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway_device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGatewayDeviceStateService implements GatewayDeviceStateService {

    private final static String DEVICE_RENAMED_METHOD_NAME = "gateway_device_renamed";
    private final static String DEVICE_DELETED_METHOD_NAME = "gateway_device_deleted";

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    protected long minTimeout;

    @Value("${server.rest.server_side_rpc.default_timeout:10000}")
    protected long defaultTimeout;

    private final RelationService relationService;
    private final DeviceService deviceService;

    @Lazy
    @Autowired
    private TbCoreDeviceRpcService deviceRpcService;

    @Override
    public void update(Device device, Device oldDevice) {
        Device gatewayDevice = findGatewayDeviceByRelationFromDevice(device);
        if (gatewayDevice != null) {
            ObjectNode renamedDeviceNode = JacksonUtil.newObjectNode();
            renamedDeviceNode.put(device.getName(), oldDevice.getName());
            ToDeviceRpcRequest rpcRequest = formDeviceToGatewayRPCRequest(gatewayDevice, renamedDeviceNode, DEVICE_RENAMED_METHOD_NAME);
            deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> {
                log.trace("Device renamed RPC with id: [{}] processed to gateway device with id: [{}], old device name: [{}], new device name: [{}]",
                        rpcRequest.getId(), gatewayDevice.getId(), oldDevice.getName(), device.getName());
            }, null);
        }
    }

    @Override
    public void delete(Device device) {
        Device gatewayDevice = findGatewayDeviceByRelationFromDevice(device);
        if (gatewayDevice != null) {
            TextNode deletedDeviceNode = new TextNode(device.getName());
            ToDeviceRpcRequest rpcRequest = formDeviceToGatewayRPCRequest(gatewayDevice, deletedDeviceNode, DEVICE_DELETED_METHOD_NAME);
            deviceRpcService.processRestApiRpcRequest(rpcRequest, fromDeviceRpcResponse -> {
                log.trace("Device deleted RPC with id: [{}] processed to gateway device with id: [{}], deleted device name: [{}]",
                        rpcRequest.getId(), gatewayDevice.getId(), device.getName());
            }, null);
        }
    }

    private ToDeviceRpcRequest formDeviceToGatewayRPCRequest(Device gatewayDevice, JsonNode deviceDataNode, String method) {
        ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(method, JacksonUtil.toString(deviceDataNode));
        long expTime = System.currentTimeMillis() + Math.max(minTimeout, defaultTimeout);
        UUID rpcRequestUUID = UUID.randomUUID();
        return new ToDeviceRpcRequest(rpcRequestUUID,
                gatewayDevice.getTenantId(),
                gatewayDevice.getId(),
                true,
                expTime,
                body,
                true,
                null,
                null
        );
    }

    private Device findGatewayDeviceByRelationFromDevice(Device device) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            EntityRelation relationToGateway = relationToGatewayList.get(0);
            return deviceService.findDeviceById(device.getTenantId(), (DeviceId) relationToGateway.getTo());
        }
        return null;
    }
}
