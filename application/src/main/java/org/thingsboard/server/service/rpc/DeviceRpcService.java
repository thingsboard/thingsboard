/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.rpc;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;

import java.util.function.Consumer;

/**
 * Created by ashvayka on 16.04.18.
 */
public interface DeviceRpcService {

    void processRestAPIRpcRequestToRuleEngine(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer);

    void processRestAPIRpcResponseFromRuleEngine(FromDeviceRpcResponse response);

    void processRpcRequestToDevice(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer);

    void processRpcResponseFromDevice(FromDeviceRpcResponse response);

    void sendRpcReplyToDevice(TenantId tenantId, DeviceId deviceId, int requestId, String body);

    void processRemoteResponseFromDevice(ServerAddress serverAddress, byte[] bytes);
}
