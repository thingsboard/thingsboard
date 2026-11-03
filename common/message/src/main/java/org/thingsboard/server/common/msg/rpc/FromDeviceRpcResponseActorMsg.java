// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.rpc;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

@Data
public class FromDeviceRpcResponseActorMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -6648120137236354987L;

    private final Integer requestId;
    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final FromDeviceRpcResponse msg;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG;
    }
}
