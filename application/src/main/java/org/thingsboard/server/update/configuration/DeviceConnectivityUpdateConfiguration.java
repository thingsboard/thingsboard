/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.thingsboard.server.dao.device.DeviceConnectivityInfo;

import java.util.HashMap;
import java.util.Map;

@Profile("update")
@Configuration
@ConfigurationProperties(prefix = "device")
@Data
public class DeviceConnectivityUpdateConfiguration {
    private Map<String, DeviceConnectivityInfo> connectivity = new HashMap<>();

    public boolean isEnabled(String protocol) {
        var info = connectivity.get(protocol);
        return info != null && info.isEnabled();
    }
}
