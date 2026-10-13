// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Long countDevices() {
        return deviceRepository.count();
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(deviceIds, ids ->
                deviceRepository.findDevicesByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupId(
                        groupId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupIds(
                        groupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        return deviceRepository.findEntityInfosByNamePrefix(tenantId.getId(), name);
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupIdsAndType(
                        groupIds,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
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
        return DaoUtil.getEntitiesByTenantIdAndIdIn(deviceIds, ids ->
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, ids), service);
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findFirstByTenantIdAndName(tenantId, name));
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
    public PageData<DeviceId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = deviceRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = deviceRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, DeviceId::new);
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
    public PageData<Device> findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(UUID groupId,
                                                                                UUID deviceProfileId,
                                                                                OtaPackageType type,
                                                                                PageLink pageLink) {
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByEntityGroupIdAndDeviceProfileIdAndFirmwareIdIsNull(
                        groupId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                () -> deviceRepository.findByEntityGroupIdAndDeviceProfileIdAndSoftwareIdIsNull(
                        groupId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                type);
        return DaoUtil.toPageData(page);
    }

    @Override
    public PageData<Device> findByDeviceProfileAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId,
                                                                  OtaPackageType type,
                                                                  PageLink pageLink) {
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                () -> deviceRepository.findByDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                type);
        return DaoUtil.toPageData(page);
    }

    @Override
    public Long countByEntityGroupAndEmptyOtaPackage(UUID groupId, UUID otaPackageId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByEntityGroupIdAndFirmwareIdIsNull(groupId, otaPackageId),
                () -> deviceRepository.countByEntityGroupIdAndSoftwareIdIsNull(groupId, otaPackageId),
                type);
    }

    @Override
    public Long countByDeviceProfileAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId),
                () -> deviceRepository.countByDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId),
                type);
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
    public EntityInfo findDeviceEntityInfoById(TenantId tenantId, DeviceId deviceId) {
        return deviceRepository.findEntityInfoById(deviceId.getId());
    }

    @Override
    public PageData<EntityInfo> findDeviceEntityInfosByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(deviceRepository.findEntityInfosByTenantIdAndProfileId(
                tenantId.getId(),
                deviceProfileId.getId(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Map<String, Long> countDevicesPerTransportType() {
        return deviceRepository.countDevicesPerTransportType().stream().collect(Collectors.toMap(e -> e.getFirst().name(), TbPair::getSecond));
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
    public List<DeviceCacheInfo> findDeviceCacheInfosByRelationType(String relationType, UUID id, int batchSize) {
        return deviceRepository.findDeviceCacheInfosByRelationType(relationType, id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
