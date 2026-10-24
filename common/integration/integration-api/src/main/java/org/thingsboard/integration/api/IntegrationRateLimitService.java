// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.function.Supplier;

public interface IntegrationRateLimitService {

    void checkLimit(TenantId tenantId, Supplier<String> msg);

    void checkLimitPerDevice(TenantId tenantId, String deviceName, Supplier<String> msg);

    void checkLimitPerAsset(TenantId tenantId, String assetName, Supplier<String> msg);

    boolean checkLimit(TenantId tenantId, IntegrationId integrationId, boolean throwException);

    boolean checkLimit(TenantId tenantId, ConverterId converterId, boolean throwException);

    boolean alreadyProcessed(EntityId entityId, EntityType entityType);
}
