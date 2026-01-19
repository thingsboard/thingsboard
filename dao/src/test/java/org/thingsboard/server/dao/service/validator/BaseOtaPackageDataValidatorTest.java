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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class BaseOtaPackageDataValidatorTest {


    DeviceProfileDao deviceProfileDao = mock(DeviceProfileDao.class);
    TenantService tenantService = mock(TenantService.class);
    BaseOtaPackageDataValidator<?> validator = spy(BaseOtaPackageDataValidator.class);
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(tenantService).given(validator).getTenantService();
        willReturn(deviceProfileDao).given(validator).getDeviceProfileDao();
    }

    @Test
    void testValidateNameInvocation() {
        OtaPackageInfo otaPackageInfo = new OtaPackageInfo();
        otaPackageInfo.setTitle("fw");
        otaPackageInfo.setVersion("1.0");
        otaPackageInfo.setType(OtaPackageType.FIRMWARE);
        otaPackageInfo.setTenantId(tenantId);

        validator.validateImpl(otaPackageInfo);
        verify(validator).validateString("OtaPackage title", otaPackageInfo.getTitle());
    }


}