// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequestMetadata {
    private final ToDeviceRpcRequestActorMsg msg;
    // Not final: the entry is registered before the send decision exists.
    private boolean sent;
    // Creation time of the persisted RPC row, captured once at create so post-persist rule-engine
    // notifications on the update paths carry the original createdTime instead of the update moment.
    private final long createdTime;
    private int retries;
    private boolean delivered;
    // False only while a persistent create is still queued for its batch insert; the send paths must not touch
    // an entry until its row is durable. Defaults true: every other path is either already durable or never persists.
    private boolean persisted = true;

    /** A persistent create still queued for its batch insert: nothing sent, nothing durable yet. */
    static ToDeviceRpcRequestMetadata awaitingPersist(ToDeviceRpcRequestActorMsg msg, long createdTime) {
        ToDeviceRpcRequestMetadata md = new ToDeviceRpcRequestMetadata(msg, createdTime);
        md.setPersisted(false);
        return md;
    }

    /** A request that is durable already, or never persists, with its send decision made. */
    static ToDeviceRpcRequestMetadata arrived(ToDeviceRpcRequestActorMsg msg, long createdTime, boolean sent) {
        ToDeviceRpcRequestMetadata md = new ToDeviceRpcRequestMetadata(msg, createdTime);
        md.setSent(sent);
        return md;
    }

    /**
     * A row reloaded on actor init: durable by definition, with sent/delivered derived from the status it was
     * persisted with. Takes the status rather than the two flags so the mapping lives in one place and there is
     * no boolean pair for a caller to transpose.
     */
    static ToDeviceRpcRequestMetadata restored(ToDeviceRpcRequestActorMsg msg, long createdTime, RpcStatus status) {
        ToDeviceRpcRequestMetadata md = arrived(msg, createdTime, status != RpcStatus.QUEUED);
        md.setDelivered(status == RpcStatus.DELIVERED);
        return md;
    }
}
