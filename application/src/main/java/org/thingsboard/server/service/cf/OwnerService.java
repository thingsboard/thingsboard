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
package org.thingsboard.server.service.cf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CustomerService customerService;

    public EntityId getOwner(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case DEVICE -> deviceService.findDeviceById(tenantId, (DeviceId) entityId).getOwnerId();
            case ASSET -> assetService.findAssetById(tenantId, (AssetId) entityId).getOwnerId();
            case CUSTOMER -> tenantId;
            default -> throw new UnsupportedOperationException();
        };
    }

    public Set<EntityId> getOwnedEntities(TenantId tenantId, EntityId ownerId) {
        Set<EntityId> ownedEntities = new HashSet<>();
        if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
            PageDataIterable<DeviceInfo> deviceIdInfos = new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).customerId((CustomerId) ownerId).build(), pageLink), 1000);
            deviceIdInfos.forEach(deviceInfo -> ownedEntities.add(deviceInfo.getId()));

            PageDataIterable<Asset> assets = new PageDataIterable<>(pageLink -> assetService.findAssetsByTenantIdAndCustomerId(tenantId, (CustomerId) ownerId, pageLink), 1000);
            assets.forEach(asset -> ownedEntities.add(asset.getId()));
        } else if (EntityType.TENANT.equals(ownerId.getEntityType())) {
            PageDataIterable<DeviceInfo> deviceIdInfos = new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId((TenantId) ownerId).customerId(new CustomerId(CustomerId.NULL_UUID)).build(), pageLink), 1000);
            deviceIdInfos.forEach(deviceInfo -> ownedEntities.add(deviceInfo.getId()));

            PageDataIterable<Asset> assets = new PageDataIterable<>(pageLink -> assetService.findAssetsByTenantIdAndCustomerId((TenantId) ownerId, new CustomerId(CustomerId.NULL_UUID), pageLink), 1000);
            assets.forEach(asset -> ownedEntities.add(asset.getId()));

            PageDataIterable<Customer> customers = new PageDataIterable<>(pageLink -> customerService.findCustomersByTenantId((TenantId) ownerId, pageLink), 1000);
            customers.forEach(customer -> ownedEntities.add(customer.getId()));
        }
        return ownedEntities;
    }

}
