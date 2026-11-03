// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface DeviceProfileService extends EntityDaoService {

    DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId, boolean putInCache);

    DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName);

    DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName, boolean putInCache);

    DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile);

    DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile, boolean doValidate, boolean publishSaveEvent);

    void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink);

    PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType);

    DeviceProfile findDeviceProfileByProvisionDeviceKey(String provisionDeviceKey);

    DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String profileName);

    DeviceProfile createDefaultDeviceProfile(TenantId tenantId);

    DeviceProfile findDefaultDeviceProfile(TenantId tenantId);

    DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId);

    boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    void deleteDeviceProfilesByTenantId(TenantId tenantId);

    List<EntityInfo> findDeviceProfileNamesByTenantId(TenantId tenantId, boolean activeOnly);

}
