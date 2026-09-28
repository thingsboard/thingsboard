// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseDeviceGroupOtaPackageService implements DeviceGroupOtaPackageService {

    private final DeviceGroupOtaPackageDao deviceGroupOtaPackageDao;
    private final OtaPackageDao otaPackageDao;
    private final EntityGroupDao entityGroupDao;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id) {
        log.trace("Executing findDeviceGroupOtaPackageById [{}]", id);
        validateId(id, uuid -> "Incorrect DeviceGroupOtaPackageId" + uuid);
        return deviceGroupOtaPackageDao.findDeviceGroupOtaPackageById(id);
    }

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(EntityGroupId groupId, OtaPackageType otaPackageType) {
        log.trace("Executing findDeviceGroupOtaPackageByGroupIdAndType [{}], [{}]", groupId, otaPackageType);
        validateId(groupId, id -> "Incorrect groupId" + id);
        return deviceGroupOtaPackageDao.findDeviceGroupOtaPackageByGroupIdAndType(groupId.getId(), otaPackageType);
    }

    @Override
    public List<DeviceGroupOtaPackage> findDeviceGroupOtaPackageByGroupId(EntityGroupId groupId) {
        log.trace("Executing findDeviceGroupOtaPackageByGroupId [{}]", groupId);
        validateId(groupId, id -> "Incorrect groupId" + id);
        return deviceGroupOtaPackageDao.findDeviceGroupOtaPackageByGroupId(groupId.getId());
    }

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        log.trace("Executing saveDeviceGroupOtaPackage [{}]", deviceGroupOtaPackage);
        deviceGroupOtaPackage.setOtaPackageUpdateTime(System.currentTimeMillis());
        validate(tenantId, deviceGroupOtaPackage);
        DeviceGroupOtaPackage result = deviceGroupOtaPackageDao.saveDeviceGroupOtaPackage(deviceGroupOtaPackage);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(result)
                .entityId(deviceGroupOtaPackage.getGroupId()).build());
        return result;
    }

    @Override
    public void deleteDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        UUID id = deviceGroupOtaPackage.getId();
        log.trace("Executing deleteDeviceGroupOtaPackage [{}]", id);
        validateId(id, uuid -> "Incorrect DeviceGroupOtaPackageId" + uuid);
        deviceGroupOtaPackageDao.deleteDeviceGroupOtaPackage(id);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(deviceGroupOtaPackage)
                .entityId(deviceGroupOtaPackage.getGroupId()).build());
    }

    private void validate(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        if (deviceGroupOtaPackage.getGroupId() == null) {
            throw new DataValidationException("DeviceGroupOtaPackage should be assigned to entity group!");
        }

        EntityGroup entityGroup = entityGroupDao.findById(tenantId, deviceGroupOtaPackage.getGroupId().getId());
        if (entityGroup == null) {
            throw new DataValidationException("OtaPackage is referencing to non-existent entity group!");
        }

        if (!entityGroup.getType().equals(EntityType.DEVICE)) {
            throw new DataValidationException("DeviceGroupOtaPackage can be only assigned to the Device group!");
        }

        if (entityGroup.getName().equals("All")) {
            throw new DataValidationException("DeviceGroupOtaPackage can`t be assigned to the group All!");
        }

        if (deviceGroupOtaPackage.getOtaPackageType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (deviceGroupOtaPackage.getId() != null) {
            DeviceGroupOtaPackage oldDeviceGroupOtaPackage = deviceGroupOtaPackageDao.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId());
            if (!deviceGroupOtaPackage.getGroupId().equals(oldDeviceGroupOtaPackage.getGroupId())) {
                throw new DataValidationException("Updating groupId is prohibited!");
            }
            if (!deviceGroupOtaPackage.getOtaPackageType().equals(oldDeviceGroupOtaPackage.getOtaPackageType())) {
                throw new DataValidationException("Updating OtaPackageType is prohibited!");
            }
        }

        if (deviceGroupOtaPackage.getOtaPackageId() == null) {
            throw new DataValidationException("DeviceGroupOtaPackage should be assigned to OtaPackage!");
        } else {
            OtaPackageInfo otaPackageInfo = otaPackageDao.findById(tenantId, deviceGroupOtaPackage.getOtaPackageId().getId());
            if (otaPackageInfo == null) {
                throw new DataValidationException("DeviceGroupOtaPackage is referencing to non-existent OtaPackage!");
            }
            if (!otaPackageInfo.getType().equals(deviceGroupOtaPackage.getOtaPackageType())) {
                throw new DataValidationException("DeviceGroupOtaPackage type should be the same as OtaPackage type!");
            }
        }
    }

}
