// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class DeviceProfileExportService extends BaseEntityExportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, DeviceProfile deviceProfile, EntityExportData<DeviceProfile> exportData) {
        deviceProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, deviceProfile.getDefaultDashboardId()));
        deviceProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, deviceProfile.getDefaultRuleChainId()));
        deviceProfile.setDefaultEdgeRuleChainId(getExternalIdOrElseInternal(ctx, deviceProfile.getDefaultEdgeRuleChainId()));
        deviceProfile.setFirmwareId(getExternalIdOrElseInternal(ctx, deviceProfile.getFirmwareId()));
        deviceProfile.setSoftwareId(getExternalIdOrElseInternal(ctx, deviceProfile.getSoftwareId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DEVICE_PROFILE);
    }

}
