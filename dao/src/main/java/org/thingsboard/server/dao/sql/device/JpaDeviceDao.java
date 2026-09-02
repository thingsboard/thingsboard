/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.device;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityInfosToDto;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
@Slf4j
public class JpaDeviceDao extends JpaAbstractDao<DeviceEntity, Device> implements DeviceDao {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NativeDeviceRepository nativeDeviceRepository;

    @Autowired
    private DeviceProfileRepository deviceProfileRepository;

    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    @Override
    protected JpaRepository<DeviceEntity, UUID> getRepository() {
        return deviceRepository;
    }

    @Override
    public DeviceInfo findDeviceInfoById(TenantId tenantId, UUID deviceId) {
        return DaoUtil.getData(deviceRepository.findDeviceInfoById(deviceId));
    }

    @Override
    public PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink) {
        if (StringUtils.isEmpty(pageLink.getTextSearch())) {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            pageLink.getTextSearch(),
                            DaoUtil.toPageable(pageLink)));
        }
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findDeviceInfosByFilter(
                        filter.getTenantId().getId(),
                        DaoUtil.getId(filter.getCustomerId()),
                        DaoUtil.getId(filter.getEdgeId()),
                        filter.getType(),
                        DaoUtil.getId(filter.getDeviceProfileId()),
                        filter.getActive() != null,
                        Boolean.TRUE.equals(filter.getActive()),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        return deviceRepository.findEntityInfosByNamePrefix(tenantId.getId(), name);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(tenantId, deviceIds)));
    }

    @Override
    public List<Device> findDevicesByIds(List<UUID> deviceIds) {
        return DaoUtil.convertDataList(deviceRepository.findDevicesByIdIn(deviceIds));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(List<UUID> deviceIds) {
        return service.submit(() -> findDevicesByIds(deviceIds));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink) {
        return DaoUtil.pageToPageData(deviceRepository.findIdsByDeviceProfileTransportType(transportType, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, deviceIds)));
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(device);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DeviceId> findDeviceIdsByTenantIdAndDeviceProfileId(UUID tenantId, UUID deviceProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                        deviceRepository.findIdsByTenantIdAndDeviceProfileId(
                                tenantId,
                                deviceProfileId,
                                pageLink.getTextSearch(),
                                DaoUtil.toPageable(pageLink)))
                .mapData(DeviceId::new);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(UUID tenantId,
                                                                           UUID deviceProfileId,
                                                                           OtaPackageType type,
                                                                           PageLink pageLink) {
        Pageable pageable = DaoUtil.toPageable(pageLink);
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByTenantIdAndTypeAndFirmwareIdIsNull(tenantId, deviceProfileId, pageable),
                () -> deviceRepository.findByTenantIdAndTypeAndSoftwareIdIsNull(tenantId, deviceProfileId, pageable),
                type
        );
        return DaoUtil.toPageData(page);
    }

    @Override
    public Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByTenantIdAndDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId),
                () -> deviceRepository.countByTenantIdAndDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId),
                type
        );
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityInfosToDto(tenantId, EntityType.DEVICE, deviceProfileRepository.findActiveTenantDeviceProfileNames(tenantId)));
    }

    @Override
    public Device findDeviceByTenantIdAndId(TenantId tenantId, UUID id) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id));
    }

    @Override
    public ListenableFuture<Device> findDeviceByTenantIdAndIdAsync(TenantId tenantId, UUID id) {
        return service.submit(() -> DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id)));
    }

    @Override
    public Long countDevicesByDeviceProfileId(TenantId tenantId, UUID deviceProfileId) {
        return deviceRepository.countByDeviceProfileId(deviceProfileId);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return deviceRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(deviceRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], edgeId [{}], type [{}] and pageLink [{}]", tenantId, edgeId, type, pageLink);
        return DaoUtil.toPageData(deviceRepository
                .findByTenantIdAndEdgeIdAndType(
                        tenantId,
                        edgeId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink) {
        log.debug("Try to find tenant device id infos by pageLink [{}]", pageLink);
        return nativeDeviceRepository.findDeviceIdInfos(DaoUtil.toPageable(pageLink));
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink) {
        log.debug("Find profile device id infos by pageLink [{}]", pageLink);
        return nativeDeviceRepository.findProfileEntityIdInfos(DaoUtil.toPageable(pageLink));
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, PageLink pageLink) {
        log.debug("Find profile device id infos by tenantId[{}], pageLink [{}]", tenantId, pageLink);
        return nativeDeviceRepository.findProfileEntityIdInfosByTenantId(tenantId, DaoUtil.toPageable(pageLink));
    }

    @Override
    public Device findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Device findByTenantIdAndName(UUID tenantId, String name) {
        return findDeviceByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Device> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findDevicesByTenantId(tenantId, pageLink);
    }

    @Override
    public DeviceId getExternalIdByInternal(DeviceId internalId) {
        return Optional.ofNullable(deviceRepository.getExternalIdById(internalId.getId()))
                .map(DeviceId::new).orElse(null);
    }

    @Override
    public PageData<Device> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<DeviceFields> findNextBatch(UUID id, int batchSize) {
        return deviceRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
