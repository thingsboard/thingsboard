// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

/**
 * Where the host of a transport in the device connectivity info comes from.
 */
public enum DeviceConnectivityHostSource {
    /** Set explicitly in the Device connectivity settings. */
    DEVICE_CONNECTIVITY,
    /** Taken from the Base URL in General settings. */
    BASE_URL,
    /** Taken from the address of the request. */
    REQUEST
}
