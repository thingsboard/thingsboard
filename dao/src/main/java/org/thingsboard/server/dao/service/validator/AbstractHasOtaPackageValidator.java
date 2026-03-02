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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasOtaPackage;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DataValidator;

public abstract class AbstractHasOtaPackageValidator<D extends BaseData<?>> extends DataValidator<D> {

    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    protected <T extends HasOtaPackage> void validateOtaPackage(TenantId tenantId, T entity, DeviceProfileId deviceProfileId) {
        if (entity.getFirmwareId() != null) {
            OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, entity.getFirmwareId());
            validateOtaPackage(tenantId, OtaPackageType.FIRMWARE, deviceProfileId, firmware);
        }
        if (entity.getSoftwareId() != null) {
            OtaPackage software = otaPackageService.findOtaPackageById(tenantId, entity.getSoftwareId());
            validateOtaPackage(tenantId, OtaPackageType.SOFTWARE, deviceProfileId, software);
        }
    }

    private void validateOtaPackage(TenantId tenantId, OtaPackageType type, DeviceProfileId deviceProfileId, OtaPackage otaPackage) {
        if (otaPackage == null) {
            throw new DataValidationException(prepareMsg("Can't assign non-existent %s!", type));
        }
        if (!otaPackage.getTenantId().equals(tenantId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s from different tenant!", type));
        }
        if (!otaPackage.getType().equals(type)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with type: " + otaPackage.getType(), type));
        }
        if (otaPackage.getData() == null && !otaPackage.hasUrl()) {
            throw new DataValidationException(prepareMsg("Can't assign %s with empty data!", type));
        }
        if (!otaPackage.getDeviceProfileId().equals(deviceProfileId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with different deviceProfile!", type));
        }
    }

    private String prepareMsg(String msg, OtaPackageType type) {
        return String.format(msg, type.name().toLowerCase());
    }
}
