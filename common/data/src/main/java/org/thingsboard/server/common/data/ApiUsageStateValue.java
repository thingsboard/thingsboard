// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

public enum ApiUsageStateValue {

    ENABLED, WARNING, DISABLED;

    public static ApiUsageStateValue toMoreRestricted(ApiUsageStateValue a, ApiUsageStateValue b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }

}
