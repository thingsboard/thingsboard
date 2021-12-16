package org.thingsboard.server.service.gateway_device;

import org.thingsboard.server.common.data.Device;

public interface GatewayDeviceStateService {

    void update(Device device, Device oldDevice);

    void delete(Device device);
}
