// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ota.OtaPackageInfoDao;

@Component
public class OtaPackageInfoDataValidator extends BaseOtaPackageDataValidator<OtaPackageInfo> {

    @Autowired
    private OtaPackageInfoDao otaPackageInfoDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, OtaPackageInfo otaPackageInfo) {
        validateImpl(otaPackageInfo);
    }

    @Override
    protected OtaPackageInfo validateUpdate(TenantId tenantId, OtaPackageInfo otaPackage) {
        OtaPackageInfo otaPackageOld = otaPackageInfoDao.findById(tenantId, otaPackage.getUuidId());
        validateUpdate(otaPackage, otaPackageOld);
        return otaPackageOld;
    }
}
