// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials;

import lombok.Data;

@Data
public class BasicMqttCredentials {

    private String clientId;
    private String userName;
    private String password;

}
