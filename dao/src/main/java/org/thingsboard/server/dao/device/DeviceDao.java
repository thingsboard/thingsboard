// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface DeviceDao.
 *
 */
public interface DeviceDao extends Dao<Device>, TenantEntityDao<Device>, ExportableCustomerEntityDao<Device, DeviceId> {

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    Device save(TenantId tenantId, Device device);

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    Device saveAndFlush(TenantId tenantId, Device device);

    /**
     * Find devices by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink);

    Long countDevices();

    /**
     * Find devices by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find device ids by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param deviceProfileId the deviceProfileId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<DeviceId> findDeviceIdsByTenantIdAndDeviceProfileId(UUID tenantId, UUID deviceProfileId, PageLink pageLink);

    /**
     * Find devices by tenantId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds);

    /**
     * Find devices by devices Ids.
     *
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    List<Device> findDevicesByIds(List<UUID> deviceIds);

    /**
     * Find devices by devices Ids.
     *
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByIdsAsync(List<UUID> deviceIds);

    PageData<Device> findDevicesByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<Device> findDevicesByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<Device> findDevicesByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink);

    /**
     * Find devices by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find devices by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find devices by tenantId, customerId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    /**
     * Find devices by tenantId and device name.
     *
     * @param tenantId the tenantId
     * @param name the device name
     * @return the optional device object
     */
    Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants device types.
     *
     * @return the list of tenant device type objects
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId);

    /**
     * Find devices by tenantId and device id.
     * @param tenantId the tenant Id
     * @param id the device Id
     * @return the device object
     */
    Device findDeviceByTenantIdAndId(TenantId tenantId, UUID id);

    /**
     * Find devices by tenantId and device id.
     * @param tenantId tenantId the tenantId
     * @param id the deviceId
     * @return the device object
     */
    ListenableFuture<Device> findDeviceByTenantIdAndIdAsync(TenantId tenantId, UUID id);

    Long countDevicesByDeviceProfileId(TenantId tenantId, UUID deviceProfileId);

    /**
     * Find devices by tenantId, profileId and page link.
     *
     * @param tenantId the tenantId
     * @param profileId the profileId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink);

    PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink);

    PageData<Device> findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(UUID groupId,
                                                                         UUID deviceProfileId,
                                                                         OtaPackageType otaPackageType,
                                                                         PageLink pageLink);

    PageData<Device> findByDeviceProfileAndEmptyOtaPackage(UUID tenantId,
                                                           UUID deviceProfileId,
                                                           OtaPackageType otaPackageType,
                                                           PageLink pageLink);

    Long countByEntityGroupAndEmptyOtaPackage(UUID groupId, UUID otaPackageId, OtaPackageType type);

    Long countByDeviceProfileAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType type);

    PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink);

    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, PageLink pageLink);

    Map<String, Long> countDevicesPerTransportType();

    EntityInfo findDeviceEntityInfoById(TenantId tenantId, DeviceId deviceId);

    PageData<EntityInfo> findDeviceEntityInfosByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

    List<DeviceCacheInfo> findDeviceCacheInfosByRelationType(String relationType, UUID id, int batchSize);

}
