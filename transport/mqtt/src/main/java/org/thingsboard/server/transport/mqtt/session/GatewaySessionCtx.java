/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.device.DeviceService;

/**
 * Created by ashvayka on 19.01.17.
 */
public class GatewaySessionCtx {

    private final Device gateway;
    private final SessionMsgProcessor processor;
    private final DeviceService deviceService;
    private final DeviceAuthService authService;
    private final Map<String, GatewayDeviceSessionCtx> devices;

    public GatewaySessionCtx(SessionMsgProcessor processor, DeviceService deviceService, DeviceAuthService authService, Device gateway) {
        this.processor = processor;
        this.deviceService = deviceService;
        this.authService = authService;
        this.gateway = gateway;
        this.devices = new HashMap<>();
    }

    public void connect(String deviceName) {
        checkDeviceName(deviceName);
        Optional<Device> deviceOpt = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), deviceName);
        Device device = deviceOpt.orElseGet(() -> {
            Device newDevice = new Device();
            newDevice.setTenantId(gateway.getTenantId());
            return deviceService.saveDevice(newDevice);
        });
        devices.put(deviceName, new GatewayDeviceSessionCtx(processor, authService, device));
    }

    public void disconnect(String deviceName) {
        checkDeviceName(deviceName);
        devices.remove(deviceName);
    }

    private void checkDeviceName(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) {
            throw new RuntimeException();
        }
    }

}
