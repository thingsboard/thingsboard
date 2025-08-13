/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.rpc;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

import java.util.UUID;

@Data
public class RemoveRpcActorMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -6112720854949677477L;

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final UUID requestId;

    @Override
    public MsgType getMsgType() {
        return MsgType.REMOVE_RPC_TO_DEVICE_ACTOR_MSG;
    }
}
