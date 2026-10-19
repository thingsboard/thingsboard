// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.credentials;

import lombok.Data;

@Data
public class ProvisionDeviceCredentialsData {
    private final String token;
    private final String clientId;
    private final String username;
    private final String password;
    private final String x509CertHash;
}
