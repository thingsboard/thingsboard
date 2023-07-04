package org.thingsboard.server.dao.device;

import lombok.Data;

@Data
public class DeviceConnectivityConfiguration {
    private String deviceConnectivityHost;
    private Integer deviceConnectivityPort;
}
