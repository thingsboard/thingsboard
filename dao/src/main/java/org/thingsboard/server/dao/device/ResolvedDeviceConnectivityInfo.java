// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

/**
 * Host and port of an enabled transport as returned by {@link DeviceConnectivityService#getConnectivityInfo},
 * with the source of the host. Kept apart from {@link DeviceConnectivityInfo}, which models the stored settings.
 */
public record ResolvedDeviceConnectivityInfo(boolean enabled, String host, String port,
                                             DeviceConnectivityHostSource hostSource) {
}
