/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.api.plugins.msg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@ToString
@RequiredArgsConstructor
public class ToDeviceRpcRequestPluginMsg implements ToDeviceActorNotificationMsg {

    private final ServerAddress serverAddress;
    @Getter
    private final PluginId pluginId;
    @Getter
    private final TenantId pluginTenantId;
    @Getter
    private final ToDeviceRpcRequest msg;

    public ToDeviceRpcRequestPluginMsg(PluginId pluginId, TenantId pluginTenantId, ToDeviceRpcRequest msg) {
        this(null, pluginId, pluginTenantId, msg);
    }

    public Optional<ServerAddress> getServerAddress() {
        return Optional.ofNullable(serverAddress);
    }

    @Override
    public DeviceId getDeviceId() {
        return msg.getDeviceId();
    }

    @Override
    public TenantId getTenantId() {
        return msg.getTenantId();
    }
}

