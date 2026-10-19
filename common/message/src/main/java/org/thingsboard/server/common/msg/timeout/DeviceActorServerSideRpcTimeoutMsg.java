// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.timeout;

import org.thingsboard.server.common.msg.MsgType;

/**
 * @author Andrew Shvayka
 */
public final class DeviceActorServerSideRpcTimeoutMsg extends TimeoutMsg<Integer> {

    public DeviceActorServerSideRpcTimeoutMsg(Integer id, long timeout) {
        super(id, timeout);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG;
    }
}
