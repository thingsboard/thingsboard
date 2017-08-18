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
package org.thingsboard.server.actors.service;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.service.cluster.discovery.DiscoveryServiceListener;
import org.thingsboard.server.service.cluster.rpc.RpcMsgListener;

public interface ActorService extends SessionMsgProcessor, WebSocketMsgProcessor, RestMsgProcessor, RpcMsgListener, DiscoveryServiceListener {

    void onPluginStateChange(TenantId tenantId, PluginId pluginId, ComponentLifecycleEvent state);

    void onRuleStateChange(TenantId tenantId, RuleId ruleId, ComponentLifecycleEvent state);

    void onCredentialsUpdate(TenantId tenantId, DeviceId deviceId);

    void onDeviceNameOrTypeUpdate(TenantId tenantId, DeviceId deviceId, String deviceName, String deviceType);
}
