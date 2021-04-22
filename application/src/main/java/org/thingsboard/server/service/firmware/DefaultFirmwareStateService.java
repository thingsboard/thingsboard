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
package org.thingsboard.server.service.firmware;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.firmware.FirmwareService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.DataConstants.FIRMWARE_CHECKSUM;
import static org.thingsboard.server.common.data.DataConstants.FIRMWARE_CHECKSUM_ALGORITHM;
import static org.thingsboard.server.common.data.DataConstants.FIRMWARE_SIZE;
import static org.thingsboard.server.common.data.DataConstants.FIRMWARE_TITLE;
import static org.thingsboard.server.common.data.DataConstants.FIRMWARE_VERSION;

@Slf4j
@Service
@TbCoreComponent
public class DefaultFirmwareStateService implements FirmwareStateService {

    private final FirmwareService firmwareService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final RuleEngineTelemetryService telemetryService;

    public DefaultFirmwareStateService(FirmwareService firmwareService, DeviceService deviceService, DeviceProfileService deviceProfileService, RuleEngineTelemetryService telemetryService) {
        this.firmwareService = firmwareService;
        this.deviceService = deviceService;
        this.deviceProfileService = deviceProfileService;
        this.telemetryService = telemetryService;
    }

    @Override
    public void update(Device device, Device oldDevice) {
        FirmwareId newFirmwareId = device.getFirmwareId();
        if (newFirmwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newFirmwareId = newDeviceProfile.getFirmwareId();
        }
        if (oldDevice != null) {
            if (newFirmwareId != null) {
                FirmwareId oldFirmwareId = oldDevice.getFirmwareId();
                if (oldFirmwareId == null) {
                    DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                    oldFirmwareId = oldDeviceProfile.getFirmwareId();
                }
                if (!newFirmwareId.equals(oldFirmwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    update(device, firmwareService.findFirmwareById(device.getTenantId(), newFirmwareId), System.currentTimeMillis());
                }
            } else {
                // Device was updated and new firmware is not set.
                remove(device);
            }
        } else if (newFirmwareId != null) {
            // Device was created and firmware is defined.
            update(device, firmwareService.findFirmwareById(device.getTenantId(), newFirmwareId), System.currentTimeMillis());
        }
    }

    @Override
    public void update(DeviceProfile deviceProfile) {
        TenantId tenantId = deviceProfile.getTenantId();

        Consumer<Device> updateConsumer;
        if (deviceProfile.getFirmwareId() != null) {
            Firmware firmware = firmwareService.findFirmwareById(tenantId, deviceProfile.getFirmwareId());
            long ts = System.currentTimeMillis();
            updateConsumer = d -> update(d, firmware, ts);
        } else {
            updateConsumer = this::remove;
        }

        PageLink pageLink = new PageLink(100);
        PageData<Device> pageData;
        do {
            //TODO: create a query which will return devices without firmware
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, deviceProfile.getName(), pageLink);

            pageData.getData().stream().filter(d -> d.getFirmwareId() == null).forEach(updateConsumer);

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    private void update(Device device, Firmware firmware, long ts) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();

        List<TsKvEntry> telemetry = new ArrayList<>();
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(DataConstants.TARGET_FIRMWARE_TITLE, firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(DataConstants.TARGET_FIRMWARE_VERSION, firmware.getVersion())));

        telemetryService.saveAndNotify(tenantId, deviceId, telemetry, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save telemetry with target firmware for device!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save telemetry with target firmware for device!", deviceId, t);
            }
        });

        List<AttributeKvEntry> attributes = new ArrayList<>();

        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(DataConstants.FIRMWARE_TITLE, firmware.getTitle())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(DataConstants.FIRMWARE_VERSION, firmware.getVersion())));

        attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(FIRMWARE_SIZE, (long) firmware.getData().array().length)));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(DataConstants.FIRMWARE_CHECKSUM_ALGORITHM, firmware.getChecksumAlgorithm())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(DataConstants.FIRMWARE_CHECKSUM, firmware.getChecksum())));
        telemetryService.saveAndNotify(tenantId, deviceId, DataConstants.SHARED_SCOPE, attributes, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save attributes with target firmware!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save attributes with target firmware!", deviceId, t);
            }
        });
    }

    private void remove(Device device) {
        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE,
                Arrays.asList(FIRMWARE_TITLE, FIRMWARE_VERSION, FIRMWARE_SIZE, FIRMWARE_CHECKSUM_ALGORITHM, FIRMWARE_CHECKSUM),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove target firmware attributes!", device.getId());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove target firmware attributes!", device.getId(), t);
                    }
                });
    }
}
