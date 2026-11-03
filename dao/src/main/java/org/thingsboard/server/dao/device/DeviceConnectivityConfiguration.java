// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

@Profile("install")
@Configuration
@ConfigurationProperties(prefix = "device")
@Data
public class DeviceConnectivityConfiguration {
    private Map<String, DeviceConnectivityInfo> connectivity = new HashMap<>();

    public DeviceConnectivityInfo getConnectivity(String protocol) {
        return connectivity.get(protocol);
    }

    public boolean isEnabled(String protocol) {
        var info = connectivity.get(protocol);
        return info != null && info.isEnabled();
    }
}
