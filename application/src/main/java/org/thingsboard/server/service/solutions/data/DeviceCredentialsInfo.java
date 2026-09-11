// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@Data
public class DeviceCredentialsInfo {

    String name;
    String type;
    DeviceCredentials credentials;
    String customerName;
    boolean gateway;

}
