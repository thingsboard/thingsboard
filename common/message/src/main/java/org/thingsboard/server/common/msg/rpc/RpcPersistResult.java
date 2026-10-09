// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.msg.rpc;

/**
 * Outcome of a batched persistent-RPC create, as reported back to the device actor.
 */
public enum RpcPersistResult {

    INSERTED,
    DUPLICATE,
    FAILED
}
