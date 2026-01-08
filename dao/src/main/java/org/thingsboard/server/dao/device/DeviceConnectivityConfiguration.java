/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
