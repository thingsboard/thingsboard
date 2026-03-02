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
package org.thingsboard.server.service.entitiy.tenant.profile;

import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.User;

public interface TbTenantProfileService {

    TenantProfile save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile, User user) throws ThingsboardException;

    void delete(TenantId tenantId, @NotNull TenantProfile tenantProfile, User user) throws ThingsboardException;

    TenantProfile setDefaultTenantProfile(TenantId tenantId, @NotNull TenantProfile tenantProfile, User user) throws ThingsboardException;
}
