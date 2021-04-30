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
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.firmware.FirmwareUtil;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
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
import org.thingsboard.server.service.queue.TbClusterService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.thingsboard.server.common.data.firmware.FirmwareKey.CHECKSUM;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.CHECKSUM_ALGORITHM;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.SIZE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.STATE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.TITLE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.TS;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.VERSION;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getAttributeKey;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getTargetTelemetryKey;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getTelemetryKey;
import static org.thingsboard.server.common.data.firmware.FirmwareType.FIRMWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareType.SOFTWARE;

@Slf4j
@Service
@TbCoreComponent
public class DefaultFirmwareStateService implements FirmwareStateService {

    private final TbClusterService tbClusterService;
    private final FirmwareService firmwareService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final RuleEngineTelemetryService telemetryService;
    private final TbQueueProducer<TbProtoQueueMsg<ToFirmwareStateServiceMsg>> fwStateMsgProducer;

    public DefaultFirmwareStateService(TbClusterService tbClusterService, FirmwareService firmwareService,
                                       DeviceService deviceService,
                                       DeviceProfileService deviceProfileService,
                                       RuleEngineTelemetryService telemetryService,
                                       TbCoreQueueFactory coreQueueFactory) {
        this.tbClusterService = tbClusterService;
        this.firmwareService = firmwareService;
        this.deviceService = deviceService;
        this.deviceProfileService = deviceProfileService;
        this.telemetryService = telemetryService;
        this.fwStateMsgProducer = coreQueueFactory.createToFirmwareStateServiceMsgProducer();
    }

    @Override
    public void update(Device device, Device oldDevice) {
        updateFirmware(device, oldDevice);
        updateSoftware(device, oldDevice);
    }

    private void updateFirmware(Device device, Device oldDevice) {
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
                    send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), FIRMWARE);
                }
            } else {
                // Device was updated and new firmware is not set.
                remove(device, FIRMWARE);
            }
        } else if (newFirmwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), FIRMWARE);
        }
    }

    private void updateSoftware(Device device, Device oldDevice) {
        FirmwareId newSoftwareId = device.getSoftwareId();
        if (newSoftwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newSoftwareId = newDeviceProfile.getSoftwareId();
        }
        if (oldDevice != null) {
            if (newSoftwareId != null) {
                FirmwareId oldSoftwareId = oldDevice.getSoftwareId();
                if (oldSoftwareId == null) {
                    DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                    oldSoftwareId = oldDeviceProfile.getSoftwareId();
                }
                if (!newSoftwareId.equals(oldSoftwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), SOFTWARE);
                }
            } else {
                // Device was updated and new firmware is not set.
                remove(device, SOFTWARE);
            }
        } else if (newSoftwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), SOFTWARE);
        }
    }

    @Override
    public void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged) {
        TenantId tenantId = deviceProfile.getTenantId();

        if (isFirmwareChanged) {
            update(tenantId, deviceProfile, FIRMWARE);
        }
        if (isSoftwareChanged) {
            update(tenantId, deviceProfile, SOFTWARE);
        }
    }

    private void update(TenantId tenantId, DeviceProfile deviceProfile, FirmwareType firmwareType) {
        Function<PageLink, PageData<Device>> getDevicesFunction;
        Consumer<Device> updateConsumer;

        switch (firmwareType) {
            case FIRMWARE:
                getDevicesFunction = pl -> deviceService.findDevicesByTenantIdAndTypeAndEmptyFirmware(tenantId, deviceProfile.getName(), pl);
                break;
            case SOFTWARE:
                getDevicesFunction = pl -> deviceService.findDevicesByTenantIdAndTypeAndEmptySoftware(tenantId, deviceProfile.getName(), pl);
                break;
            default:
                log.warn("Unsupported firmware type: [{}]", firmwareType);
                return;
        }

        if (deviceProfile.getFirmwareId() != null) {
            long ts = System.currentTimeMillis();
            updateConsumer = d -> send(d.getTenantId(), d.getId(), deviceProfile.getFirmwareId(), ts, firmwareType);
        } else {
            updateConsumer = d -> remove(d, firmwareType);
        }

        PageLink pageLink = new PageLink(100);
        PageData<Device> pageData;
        do {
            pageData = getDevicesFunction.apply(pageLink);
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
        FirmwareType firmwareType = FirmwareType.valueOf(msg.getType());
        long ts = msg.getTs();

        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("[{}] [{}] Device was removed during firmware update msg was queued!", tenantId, deviceId);
        } else {
            FirmwareId currentFirmwareId = FirmwareUtil.getFirmwareId(device, firmwareType);
            if (currentFirmwareId == null) {
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, device.getDeviceProfileId());
                currentFirmwareId = FirmwareUtil.getFirmwareId(deviceProfile, firmwareType);
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

    private void send(TenantId tenantId, DeviceId deviceId, FirmwareId firmwareId, long ts, FirmwareType firmwareType) {
        ToFirmwareStateServiceMsg msg = ToFirmwareStateServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setFirmwareIdMSB(firmwareId.getId().getMostSignificantBits())
                .setFirmwareIdLSB(firmwareId.getId().getLeastSignificantBits())
                .setType(firmwareType.name())
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
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), TITLE), firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), VERSION), firmware.getVersion())));
        telemetry.add(new BasicTsKvEntry(ts, new LongDataEntry(getTargetTelemetryKey(firmware.getType(), TS), ts)));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTelemetryKey(firmware.getType(), STATE), FirmwareUpdateStatus.QUEUED.name())));

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

        BasicTsKvEntry status = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(getTelemetryKey(firmware.getType(), STATE), FirmwareUpdateStatus.INITIATED.name()));

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
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), TITLE), firmware.getTitle())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), VERSION), firmware.getVersion())));
        attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(getAttributeKey(firmware.getType(), SIZE), firmware.getDataSize())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), CHECKSUM_ALGORITHM), firmware.getChecksumAlgorithm())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), CHECKSUM), firmware.getChecksum())));

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

    private void remove(Device device, FirmwareType firmwareType) {
        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, FirmwareUtil.getAttributeKeys(firmwareType),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove target firmware attributes!", device.getId());
                        Set<AttributeKey> keysToNotify = new HashSet<>();
                        FirmwareUtil.ALL_FW_ATTRIBUTE_KEYS.forEach(key -> keysToNotify.add(new AttributeKey(DataConstants.SHARED_SCOPE, key)));
                        tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), device.getId(), keysToNotify), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove target firmware attributes!", device.getId(), t);
                    }
                });
    }
}
