// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@TbTransportComponent
@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceProtoService {

    private final TransportService transportService;

    public Device getDeviceById(DeviceId id) {
        TransportProtos.GetDeviceResponseMsg deviceProto = transportService.getDevice(TransportProtos.GetDeviceRequestMsg.newBuilder()
                .setDeviceIdMSB(id.getId().getMostSignificantBits())
                .setDeviceIdLSB(id.getId().getLeastSignificantBits())
                .build());

        if (deviceProto == null) {
            return null;
        }

        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(
                deviceProto.getDeviceProfileIdMSB(), deviceProto.getDeviceProfileIdLSB())
        );

        Device device = new Device();
        device.setId(id);
        device.setDeviceProfileId(deviceProfileId);

        DeviceTransportConfiguration deviceTransportConfiguration = JacksonUtil.fromBytes(
                deviceProto.getDeviceTransportConfiguration().toByteArray(), DeviceTransportConfiguration.class);

        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(deviceTransportConfiguration);
        device.setDeviceData(deviceData);

        return device;
    }

    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        TransportProtos.GetDeviceCredentialsResponseMsg deviceCredentialsResponse = transportService.getDeviceCredentials(
                TransportProtos.GetDeviceCredentialsRequestMsg.newBuilder()
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .build()
        );

        if (deviceCredentialsResponse.hasDeviceCredentialsData()) {
            return ProtoUtils.fromProto(deviceCredentialsResponse.getDeviceCredentialsData());
        } else {
            throw new IllegalArgumentException("Device credentials not found");
        }
    }

    /**
     * Pages through all device ids of the given transport type and passes each one to the consumer.
     * The caller keeps its own (thread-safe) set of ids inside the consumer: device events may update it while this runs.
     */
    public void forEachDeviceIdByTransportType(
            String jobName, DeviceTransportType deviceTransportType, int batchRetries, Consumer<DeviceId> consumer) {

        int batchIndex = 0;
        int batchSize = 512;
        boolean nextBatchExists = true;
        int processed = 0;

        while (nextBatchExists) {
            for (int attempt = 1; attempt <= batchRetries; attempt++) {
                try {
                    TransportProtos.GetDeviceIdsByTransportTypeResponseMsg devicesResponse = getDevicesIds(deviceTransportType, batchIndex, batchSize);
                    devicesResponse.getIdsList().stream()
                            .map(id -> new DeviceId(UUID.fromString(id)))
                            .forEach(consumer);
                    processed += devicesResponse.getIdsCount();
                    nextBatchExists = devicesResponse.getHasNextPage();
                    batchIndex++;
                    break;
                } catch (Exception e) {
                    if (attempt >= batchRetries) {
                        log.error("{} {}: batch {} failed after {} attempts.", deviceTransportType, jobName, batchIndex, attempt, e);
                        return;
                    }
                    log.warn("{} {}: batch {} attempt {}/{} failed.", deviceTransportType, jobName, batchIndex, attempt, batchRetries, e);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException ex) {
                        log.warn("{} {} interrupted. Stopping bootstrap task.", deviceTransportType, jobName);
                        return;
                    }
                }
            }
        }
        log.debug("{} {}: processed {} device ids", deviceTransportType, jobName, processed);
    }

    public TransportProtos.GetDeviceIdsByTransportTypeResponseMsg getDevicesIds(DeviceTransportType transportType, int page, int pageSize) {
        return transportService.getDeviceIdsByTransportType(transportType, page, pageSize);
    }
}
