// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.rule.engine;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

@Data
public class DeviceEdgeUpdateMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = 4679029228395462172L;

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final EdgeId edgeId;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
