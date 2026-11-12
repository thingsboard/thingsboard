// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rpc;

import lombok.Getter;

public enum RpcStatus {

    QUEUED(true),
    SENT(true),
    DELIVERED(true),
    SUCCESSFUL(false),
    TIMEOUT(false),
    EXPIRED(false),
    FAILED(false),
    DELETED(false);

    /**
     * {@code true} while the RPC is still pending in the device actor (not yet delivered/answered).
     * Note: TIMEOUT is not intermediate, but it is not final either - the device actor may retry
     * the delivery (TIMEOUT -> SENT/DELIVERED/QUEUED) until retries are exhausted (then FAILED).
     */
    @Getter
    private final boolean intermediate;

    RpcStatus(boolean intermediate) {
        this.intermediate = intermediate;
    }

}
