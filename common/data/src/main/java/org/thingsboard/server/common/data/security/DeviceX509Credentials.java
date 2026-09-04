// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security;

/**
 * @author Valerii Sosliuk
 */
public class DeviceX509Credentials implements DeviceCredentialsFilter {

    private final String sha3Hash;

    public DeviceX509Credentials(String sha3Hash) {
        this.sha3Hash = sha3Hash;
    }

    @Override
    public String getCredentialsId() { return sha3Hash; }

    @Override
    public DeviceCredentialsType getCredentialsType() { return DeviceCredentialsType.X509_CERTIFICATE; }

    @Override
    public String toString() {
        return "DeviceX509Credentials [SHA3=" + sha3Hash + "]";
    }
}
