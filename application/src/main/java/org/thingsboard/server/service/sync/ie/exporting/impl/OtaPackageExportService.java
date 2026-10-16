// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.sync.ie.OtaPackageExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class OtaPackageExportService extends BaseEntityExportService<OtaPackageId, OtaPackage, OtaPackageExportData> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, OtaPackage otaPackage, OtaPackageExportData exportData) {
        otaPackage.setDeviceProfileId(getExternalIdOrElseInternal(ctx, otaPackage.getDeviceProfileId()));
    }

    @Override
    protected OtaPackageExportData newExportData() {
        return new OtaPackageExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.OTA_PACKAGE);
    }

}
