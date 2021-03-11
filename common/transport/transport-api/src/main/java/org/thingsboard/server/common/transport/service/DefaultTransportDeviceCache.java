/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@TbTransportComponent
@RequiredArgsConstructor
public class DefaultTransportDeviceCache implements TransportDeviceCache {
    private final TransportService transportService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final Map<DeviceId, Device> devices = new ConcurrentHashMap<>();

    @Override
    public Device get(DeviceId id) {
        return Optional.ofNullable(devices.get(id))
                .orElseGet(() -> {
                    Device device = requestDevice(id);
                    devices.put(id, device);
                    return device;
                });
    }

    private Device requestDevice(DeviceId id) {
        TransportProtos.GetDeviceResponseMsg deviceProto = transportService.getDevice(TransportProtos.GetDeviceRequestMsg.newBuilder()
                .setDeviceIdMSB(id.getId().getMostSignificantBits())
                .setDeviceIdLSB(id.getId().getLeastSignificantBits())
                .build());

        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(
                deviceProto.getDeviceProfileIdMSB(), deviceProto.getDeviceProfileIdLSB())
        );

        Device device = new Device();
        device.setId(id);
        device.setDeviceProfileId(deviceProfileId);

        SnmpDeviceTransportConfiguration snmpDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) dataDecodingEncodingService.decode(
                deviceProto.getDeviceTransportConfiguration().toByteArray()
        ).orElseThrow(() -> new IllegalStateException("Can't find device transport configuration"));

        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(snmpDeviceTransportConfiguration);
        device.setDeviceData(deviceData);

        return device;
    }

    @Override
    public void evict(DeviceId id) {
        devices.remove(id);
    }
}
