// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ota;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DeviceGroupOtaPackageEntity;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@SqlDao
public class JpaDeviceGroupOtaPackageDao implements DeviceGroupOtaPackageDao {

    private final DeviceGroupOtaPackageRepository deviceGroupOtaPackageRepository;

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id) {
        return DaoUtil.getData(deviceGroupOtaPackageRepository.findById(id));
    }

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(UUID groupId, OtaPackageType type) {
        return DaoUtil.getData(deviceGroupOtaPackageRepository.findByGroupIdAndOtaPackageType(groupId, type));
    }

    @Override
    public List<DeviceGroupOtaPackage> findDeviceGroupOtaPackageByGroupId(UUID groupId) {
        return DaoUtil.convertDataList(deviceGroupOtaPackageRepository.findByGroupId(groupId));
    }

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(DeviceGroupOtaPackage deviceGroupOtaPackage) {
        if (deviceGroupOtaPackage.getId() == null) {
            UUID uuid = Uuids.timeBased();
            deviceGroupOtaPackage.setId(uuid);
        }
        return DaoUtil.getData(deviceGroupOtaPackageRepository.save(new DeviceGroupOtaPackageEntity(deviceGroupOtaPackage)));
    }

    @Override
    public boolean deleteDeviceGroupOtaPackage(UUID id) {
        deviceGroupOtaPackageRepository.deleteById(id);
        log.debug("Remove request: {}", id);
        return !deviceGroupOtaPackageRepository.existsById(id);
    }

    @Override
    public PageData<DeviceGroupOtaPackage> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(deviceGroupOtaPackageRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

}
