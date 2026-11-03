// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.transport.msg;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ashvayka on 09.10.18.
 */
@Data
public class TransportToDeviceActorMsgWrapper implements TbActorMsg, DeviceAwareMsg, TenantAwareMsg, Serializable {

    private static final long serialVersionUID = 7191333353202935941L;

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final TransportToDeviceActorMsg msg;
    private final TbCallback callback;

    public TransportToDeviceActorMsgWrapper(TransportToDeviceActorMsg msg, TbCallback callback) {
        this.msg = msg;
        this.callback = callback;
        this.tenantId = TenantId.fromUUID(new UUID(msg.getSessionInfo().getTenantIdMSB(), msg.getSessionInfo().getTenantIdLSB()));
        this.deviceId = new DeviceId(new UUID(msg.getSessionInfo().getDeviceIdMSB(), msg.getSessionInfo().getDeviceIdLSB()));
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG;
    }
}
