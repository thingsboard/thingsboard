// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.Data;

@Data
public class DeviceConnectivityInfo {
    private boolean enabled;
    private String host;
    private String port;
}
