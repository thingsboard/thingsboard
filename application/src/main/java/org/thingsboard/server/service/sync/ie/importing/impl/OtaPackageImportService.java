// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.OtaPackageExportData;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class OtaPackageImportService extends BaseEntityImportService<OtaPackageId, OtaPackage, OtaPackageExportData> {

    private final OtaPackageService otaPackageService;

    @Override
    protected void setOwner(TenantId tenantId, OtaPackage otaPackage, IdProvider idProvider) {
        otaPackage.setTenantId(tenantId);
    }

    @Override
    protected OtaPackage prepare(EntitiesImportCtx ctx, OtaPackage otaPackage, OtaPackage oldOtaPackage, OtaPackageExportData exportData, IdProvider idProvider) {
        otaPackage.setDeviceProfileId(idProvider.getInternalId(otaPackage.getDeviceProfileId()));
        return otaPackage;
    }

    @Override
    protected OtaPackage findExistingEntity(EntitiesImportCtx ctx, OtaPackage otaPackage, IdProvider idProvider) {
        OtaPackage existingOtaPackage = super.findExistingEntity(ctx, otaPackage, idProvider);
        if (existingOtaPackage == null && ctx.isFindExistingByName()) {
            existingOtaPackage = otaPackageService.findOtaPackageByTenantIdAndTitleAndVersion(ctx.getTenantId(), otaPackage.getTitle(), otaPackage.getVersion());
        }
        return existingOtaPackage;
    }

    @Override
    protected OtaPackage deepCopy(OtaPackage otaPackage) {
        return new OtaPackage(otaPackage);
    }

    @Override
    protected OtaPackage saveOrUpdate(EntitiesImportCtx ctx, OtaPackage otaPackage, OtaPackageExportData exportData, IdProvider idProvider, CompareResult compareResult) {
        if (otaPackage.hasUrl()) {
            OtaPackageInfo info = new OtaPackageInfo(otaPackage);
            return new OtaPackage(otaPackageService.saveOtaPackageInfo(info, info.hasUrl()));
        }
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
