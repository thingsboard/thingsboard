// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.subscription;

public enum SubscriptionErrorCode {

    LIMIT_REACHED(1),
    FEATURE_DISABLED(2);

    private int errorCode;

    SubscriptionErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
