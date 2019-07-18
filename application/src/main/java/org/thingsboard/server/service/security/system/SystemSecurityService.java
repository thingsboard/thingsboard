/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.security.system;

import org.springframework.security.core.AuthenticationException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.service.security.model.SecuritySettings;

public interface SystemSecurityService {

    SecuritySettings getSecuritySettings(TenantId tenantId);

    SecuritySettings saveSecuritySettings(TenantId tenantId, SecuritySettings securitySettings);

    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String password) throws AuthenticationException;

    void validatePassword(TenantId tenantId, String password) throws DataValidationException;

}
