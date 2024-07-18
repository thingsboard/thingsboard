/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class MobileAppDataValidator extends DataValidator<MobileApp> {

    @Override
    protected void validateDataImpl(TenantId tenantId, MobileApp mobileApp) {
        if (StringUtils.isEmpty(mobileApp.getPkgName())) {
            throw new DataValidationException("Package should be specified!");
        }
        if (StringUtils.isEmpty(mobileApp.getAppSecret())) {
            throw new DataValidationException("Application secret should be specified!");
        }
        if (mobileApp.getAppSecret().length() < 16) {
            throw new DataValidationException("Application secret should be at least 16 characters!");
        }
    }
}
