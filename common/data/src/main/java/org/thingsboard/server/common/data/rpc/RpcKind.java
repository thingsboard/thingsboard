// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.rpc;

/**
 * Whether an RPC expects a response from the device. The two kinds have different terminal states, so the
 * status state machine in {@link RpcStatus#getAllowedFromStatuses(RpcKind)} differs between them.
 */
public enum RpcKind {

    ONE_WAY, TWO_WAY;

    /**
     * The persisted {@code rpc.oneway} column is nullable -- rows written before it existed carry NULL -- and a
     * row without the flag is treated as two-way, so a real response can still complete it.
     */
    public static RpcKind of(Boolean oneway) {
        return Boolean.TRUE.equals(oneway) ? ONE_WAY : TWO_WAY;
    }
}
