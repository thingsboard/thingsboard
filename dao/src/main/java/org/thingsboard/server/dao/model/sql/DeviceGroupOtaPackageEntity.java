// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_UPDATE_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_GROUP_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_TABLE_NAME;

@Data
@Entity
@Table(name = DEVICE_GROUP_OTA_PACKAGE_TABLE_NAME)
public class DeviceGroupOtaPackageEntity implements ToData<DeviceGroupOtaPackage> {
    @Id
    @Column(name = DEVICE_GROUP_OTA_PACKAGE_ID, columnDefinition = "uuid")
    private UUID id;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_GROUP_ID, columnDefinition = "uuid")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_TYPE)
    private OtaPackageType otaPackageType;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_ID, columnDefinition = "uuid")
    private UUID otaPackageId;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_UPDATE_TIME)
    private long otaPackageUpdateTime;

    public DeviceGroupOtaPackageEntity() {
        super();
    }

    public DeviceGroupOtaPackageEntity(DeviceGroupOtaPackage deviceGroupOtaPackage) {
        this.id = deviceGroupOtaPackage.getId();
        this.groupId = deviceGroupOtaPackage.getGroupId().getId();
        this.otaPackageType = deviceGroupOtaPackage.getOtaPackageType();
        this.otaPackageId = deviceGroupOtaPackage.getOtaPackageId().getId();
        this.otaPackageUpdateTime = deviceGroupOtaPackage.getOtaPackageUpdateTime();
    }

    @Override
    public DeviceGroupOtaPackage toData() {
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setId(id);
        deviceGroupOtaPackage.setGroupId(new EntityGroupId(groupId));
        deviceGroupOtaPackage.setOtaPackageType(otaPackageType);
        deviceGroupOtaPackage.setOtaPackageId(new OtaPackageId(otaPackageId));
        deviceGroupOtaPackage.setOtaPackageUpdateTime(otaPackageUpdateTime);
        return deviceGroupOtaPackage;
    }
}
