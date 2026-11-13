// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class AssetDataValidator extends DataValidator<Asset> {

    @Autowired
    private AssetDao assetDao;

    @Autowired
    @Lazy
    private AssetService assetService;

    @Autowired
    @Lazy
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private SubscriptionService subscriptionService;

    @Override
    protected void validateCreate(TenantId tenantId, Asset asset) {
        subscriptionService.createAssetAllowed(asset.getTenantId());
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.ASSET);
        validateNameUniqueness(asset, null, assetService::findAssetByTenantIdAndName, "Asset with such name already exists!");
        validateExternalIdUniqueness(asset, null, assetDao, "Asset with such external id already exists!");
    }

    @Override
    protected Asset validateUpdate(TenantId tenantId, Asset asset) {
        Asset old = assetDao.findById(asset.getTenantId(), asset.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset!");
        }
        validateNameUniqueness(asset, old, assetService::findAssetByTenantIdAndName, "Asset with such name already exists!");
        validateExternalIdUniqueness(asset, old, assetDao, "Asset with such external id already exists!");
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Asset asset) {
        validateString("Asset name", asset.getName());
        if (asset.getTenantId() == null) {
            throw new DataValidationException("Asset should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(asset.getTenantId())) {
                throw new DataValidationException("Asset is referencing to non-existent tenant!");
            }
        }
        if (asset.getCustomerId() == null) {
            asset.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign asset to non-existent customer!");
            }
            if (!customer.getTenantId().equals(asset.getTenantId())) {
                throw new DataValidationException("Can't assign asset to customer from different tenant!");
            }
        }
    }
}
