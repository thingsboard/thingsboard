// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.msg.rpc;

import lombok.Data;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.UUID;

/**
 * Second half of a persistent RPC request: the batch write for {@code rpcId} has settled, so the device actor
 * may now reply the rpcId and send the command.
 */
@Data
public class RpcPersistResultActorMsg implements TbActorMsg {

    private final UUID rpcId;
    private final int requestId;
    private final RpcPersistResult result;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_PERSIST_RESULT_TO_DEVICE_ACTOR_MSG;
    }
}
