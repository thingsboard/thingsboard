// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.usagerecord;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.function.Function;

public interface ApiLimitService {

    boolean checkEntitiesLimit(TenantId tenantId, EntityType entityType);

    long getLimit(TenantId tenantId, Function<DefaultTenantProfileConfiguration, Number> extractor);

}
