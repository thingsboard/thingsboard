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
package org.thingsboard.server.service.queue;

import org.thingsboard.rule.engine.api.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;

public interface TbClusterService {

    void onToRuleEngineMsg(TenantId tenantId, EntityId entityId, TbMsg msg);

    void onToCoreMsg(ToDeviceActorNotificationMsg msg);

    void onToCoreMsg(String targetServiceId, FromDeviceRpcResponse response);

    void onToRuleEngineMsg(String targetServiceId, FromDeviceRpcResponse response);

    void onEntityStateChange(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);

}
