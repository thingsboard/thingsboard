// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.security;

public class DeviceTokenCredentials implements DeviceCredentialsFilter {

    private final String token;

    public DeviceTokenCredentials(String token) {
        this.token = token;
    }

    @Override
    public DeviceCredentialsType getCredentialsType() {
        return DeviceCredentialsType.ACCESS_TOKEN;
    }

    @Override
    public String getCredentialsId() {
        return token;
    }

    @Override
    public String toString() {
        return "DeviceTokenCredentials [token=" + token + "]";
    }

}
