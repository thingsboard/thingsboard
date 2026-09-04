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

    @Getter
    private final boolean pushDeleteNotificationToCore;

    RpcStatus(boolean pushDeleteNotificationToCore) {
        this.pushDeleteNotificationToCore = pushDeleteNotificationToCore;
    }

}
