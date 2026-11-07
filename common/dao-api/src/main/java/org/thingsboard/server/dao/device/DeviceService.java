// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface DeviceService extends EntityDaoService {

    Device findDeviceById(TenantId tenantId, DeviceId deviceId);

    DeviceInfo findDeviceInfoById(TenantId tenantId, DeviceId deviceId);

    EntityInfo findDeviceEntityInfoById(TenantId tenantId, DeviceId deviceId);

    ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId);

    Device findDeviceByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<Device> findDeviceByTenantIdAndNameAsync(TenantId tenantId, String name);

    Device saveDevice(Device device);

    Device saveDevice(Device device, boolean doValidate);

    Device saveDeviceWithAccessToken(Device device, String accessToken);

    Device saveDeviceWithAccessToken(Device device, String accessToken, NameConflictStrategy nameConflictStrategy);

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials);

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, NameConflictStrategy nameConflictStrategy);

    Device saveDevice(ProvisionRequest provisionRequest, DeviceProfile profile);

    void deleteDevice(TenantId tenantId, DeviceId deviceId);

    PageData<Device> findDevicesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    Long countDevices();

    PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findAllDeviceIdInfos(int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink);

    /**
     * Passes all devices to {@code pageConsumer} page by page: keyset pages of up to {@code pageSize} rows by id, with no
     * OFFSET and no count. A page is handed over after its query has finished, so no database connection or transaction is
     * held while the consumer runs.
     */
    void findAllDeviceIdInfos(int pageSize, Consumer<List<DeviceIdInfo>> pageConsumer);

    PageData<DeviceId> findDeviceIdsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findProfileEntityIdInfosByTenantId(TenantId, int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findProfileEntityIdInfosByTenantId(TenantId, int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Same as {@link #findAllDeviceIdInfos(int, Consumer)}, limited to one tenant (keyset pages over the {@code (tenant_id, id)} index).
     */
    void findProfileEntityIdInfosByTenantId(TenantId tenantId, int pageSize, Consumer<List<ProfileEntityIdInfo>> pageConsumer);

    PageData<DeviceId> findDeviceIdsByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

    PageData<EntityInfo> findDeviceEntityInfosByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds);

    List<Device> findDevicesByIds(List<DeviceId> deviceIds);

    ListenableFuture<List<Device>> findDevicesByIdsAsync(List<DeviceId> deviceIds);

    void deleteDevicesByTenantId(TenantId tenantId);

    PageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    void deleteDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds);

    ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query);

    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId);

    PageData<Device> findDevicesByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<Device> findDevicesByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink);

    PageData<Device> findDevicesByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink);

    Device assignDeviceToTenant(TenantId tenantId, Device device);

    PageData<Device> findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(EntityGroupId groupId,
                                                                         DeviceProfileId deviceProfileId,
                                                                         OtaPackageType type,
                                                                         PageLink pageLink);

    PageData<Device> findByDeviceProfileAndEmptyOtaPackage(TenantId tenantId,
                                                           DeviceProfileId deviceProfileId,
                                                           OtaPackageType type,
                                                           PageLink pageLink);

    long countByEntityGroupAndEmptyOtaPackage(EntityGroupId groupId, OtaPackageId otaPackageId, OtaPackageType type);

    long countByDeviceProfileAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type);

    List<DeviceCacheInfo> findDeviceCacheInfosByRelationType(String relationType, UUID id, int batchSize);

}
