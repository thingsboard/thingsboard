/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.alarm.rule;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DynamicPredicateValueCtxImpl implements DynamicPredicateValueCtx {
    private final TenantId tenantId;
    private CustomerId customerId;
    private final EntityId entityId;
    private final TbAlarmRuleContext ctx;

    public DynamicPredicateValueCtxImpl(TenantId tenantId, EntityId entityId, TbAlarmRuleContext ctx) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.ctx = ctx;
        resetCustomer();
    }

    @Override
    public EntityKeyValue getTenantValue(String key) {
        return getValue(tenantId, key);
    }

    @Override
    public EntityKeyValue getCustomerValue(String key) {
        return customerId == null || customerId.isNullUid() ? null : getValue(customerId, key);
    }

    @Override
    public void resetCustomer() {
        HasCustomerId customerEntity;
        if (entityId.getEntityType() == EntityType.DEVICE) {
            customerEntity = ctx.getDeviceService().findDeviceById(tenantId, (DeviceId) entityId);
        } else {
            customerEntity = ctx.getAssetService().findAssetById(tenantId, (AssetId) entityId);
        }

        if (customerEntity != null) {
            this.customerId = customerEntity.getCustomerId();
        }
    }

    private EntityKeyValue getValue(EntityId entityId, String key) {
        try {
            Optional<AttributeKvEntry> entry = ctx.getAttributesService().find(tenantId, entityId, DataConstants.SERVER_SCOPE, key).get();
            if (entry.isPresent()) {
                return EntityState.toEntityValue(entry.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to get attribute by key: {} for {}: [{}]", key, entityId.getEntityType(), entityId.getId());
        }
        return null;
    }
}
