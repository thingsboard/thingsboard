// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.limits;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;

public interface RateLimitService {

    boolean checkRateLimit(LimitedApi api, TenantId tenantId);

    boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level);

    boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level, boolean ignoreTenantNotFound);

    boolean checkRateLimit(LimitedApi api, Object level, String rateLimitConfig);

    void cleanUp(LimitedApi api, Object level);

}
