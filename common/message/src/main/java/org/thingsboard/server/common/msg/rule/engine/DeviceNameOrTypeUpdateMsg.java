// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.rule.engine;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

@Data
public class DeviceNameOrTypeUpdateMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -5738949227650536685L;

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final String deviceName;
    private final String deviceType;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
