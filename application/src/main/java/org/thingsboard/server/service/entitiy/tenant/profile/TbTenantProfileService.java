// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
