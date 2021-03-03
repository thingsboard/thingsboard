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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface DeviceDao.
 *
 */
public interface DeviceDao extends Dao<Device>, TenantEntityDao {

    /**
     * Find device info by id.
     *
     * @param tenantId the tenant id
     * @param deviceId the device id
     * @return the device info object
     */
    DeviceInfo findDeviceInfoById(TenantId tenantId, UUID deviceId);

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    Device save(TenantId tenantId, Device device);

    /**
     * Find devices by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find device infos by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantId(UUID tenantId, PageLink pageLink);

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
     * Find device infos by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find device infos by tenantId, deviceProfileId and page link.
     *
     * @param tenantId the tenantId
     * @param deviceProfileId the deviceProfileId
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantIdAndDeviceProfileId(UUID tenantId, UUID deviceProfileId, PageLink pageLink);

    /**
     * Find devices by tenantId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds);

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
     * Find device infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

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
     * Find device infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * Find device infos by tenantId, customerId, deviceProfileId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param deviceProfileId the deviceProfileId
     * @param pageLink the page link
     * @return the list of device info objects
     */
    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(UUID tenantId, UUID customerId, UUID deviceProfileId, PageLink pageLink);

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


    /**
     * Find devices by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    /**
     * Find devices by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, PageLink pageLink);
}
