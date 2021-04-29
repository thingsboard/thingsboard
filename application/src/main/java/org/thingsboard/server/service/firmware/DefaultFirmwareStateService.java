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
import org.thingsboard.server.common.data.FirmwareInfo;
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
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.firmware.FirmwareService;
import org.thingsboard.server.gen.transport.TransportProtos.ToFirmwareStateServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
    private final TbQueueProducer<TbProtoQueueMsg<ToFirmwareStateServiceMsg>> fwStateMsgProducer;

    public DefaultFirmwareStateService(FirmwareService firmwareService,
                                       DeviceService deviceService,
                                       DeviceProfileService deviceProfileService,
                                       RuleEngineTelemetryService telemetryService,
                                       TbCoreQueueFactory coreQueueFactory) {
        this.firmwareService = firmwareService;
        this.deviceService = deviceService;
        this.deviceProfileService = deviceProfileService;
        this.telemetryService = telemetryService;
        this.fwStateMsgProducer = coreQueueFactory.createToFirmwareStateServiceMsgProducer();
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
                    send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis());
                }
            } else {
                // Device was updated and new firmware is not set.
                remove(device);
            }
        } else if (newFirmwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis());
        }
    }

    @Override
    public void update(DeviceProfile deviceProfile) {
        TenantId tenantId = deviceProfile.getTenantId();

        Consumer<Device> updateConsumer;
        if (deviceProfile.getFirmwareId() != null) {
            long ts = System.currentTimeMillis();
            updateConsumer = d -> send(d.getTenantId(), d.getId(), deviceProfile.getFirmwareId(), ts);
        } else {
            updateConsumer = this::remove;
        }

        PageLink pageLink = new PageLink(100);
        PageData<Device> pageData;
        do {
            pageData = deviceService.findDevicesByTenantIdAndTypeAndEmptyFirmware(tenantId, deviceProfile.getName(), pageLink);

            pageData.getData().forEach(updateConsumer);

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public boolean process(ToFirmwareStateServiceMsg msg) {
        boolean isSuccess = false;
        FirmwareId targetFirmwareId = new FirmwareId(new UUID(msg.getFirmwareIdMSB(), msg.getFirmwareIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        TenantId tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        long ts = msg.getTs();

        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("[{}] [{}] Device was removed during firmware update msg was queued!", tenantId, deviceId);
        } else {
            FirmwareId currentFirmwareId = device.getFirmwareId();

            if (currentFirmwareId == null) {
                currentFirmwareId = deviceProfileService.findDeviceProfileById(tenantId, device.getDeviceProfileId()).getFirmwareId();
            }

            if (targetFirmwareId.equals(currentFirmwareId)) {
                update(device, firmwareService.findFirmwareInfoById(device.getTenantId(), targetFirmwareId), ts);
                isSuccess = true;
            } else {
                log.warn("[{}] [{}] Can`t update firmware for the device, target firmwareId: [{}], current firmwareId: [{}]!", tenantId, deviceId, targetFirmwareId, currentFirmwareId);
            }
        }
        return isSuccess;
    }

    private void send(TenantId tenantId, DeviceId deviceId, FirmwareId firmwareId, long ts) {
        ToFirmwareStateServiceMsg msg = ToFirmwareStateServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setFirmwareIdMSB(firmwareId.getId().getMostSignificantBits())
                .setFirmwareIdLSB(firmwareId.getId().getLeastSignificantBits())
                .setTs(ts)
                .build();

        FirmwareInfo firmware = firmwareService.findFirmwareInfoById(tenantId, firmwareId);
        if (firmware == null) {
            log.warn("[{}] Failed to send firmware update because firmware was already deleted", firmwareId);
            return;
        }

        TopicPartitionInfo tpi = new TopicPartitionInfo(fwStateMsgProducer.getDefaultTopic(), null, null, false);
        fwStateMsgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);

        List<TsKvEntry> telemetry = new ArrayList<>();
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(DataConstants.TARGET_FIRMWARE_TITLE, firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(DataConstants.TARGET_FIRMWARE_VERSION, firmware.getVersion())));
        telemetry.add(new BasicTsKvEntry(ts, new LongDataEntry(DataConstants.TARGET_FIRMWARE_TS, ts)));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(DataConstants.FIRMWARE_STATE, FirmwareUpdateStatus.QUEUED.name())));

        telemetryService.saveAndNotify(tenantId, deviceId, telemetry, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save firmware status!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save firmware status!", deviceId, t);
            }
        });
    }


    private void update(Device device, FirmwareInfo firmware, long ts) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();

        BasicTsKvEntry status = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(DataConstants.FIRMWARE_STATE, FirmwareUpdateStatus.INITIATED.name()));

        telemetryService.saveAndNotify(tenantId, deviceId, Collections.singletonList(status), new FutureCallback<>() {
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

        attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(FIRMWARE_SIZE, firmware.getDataSize())));
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
