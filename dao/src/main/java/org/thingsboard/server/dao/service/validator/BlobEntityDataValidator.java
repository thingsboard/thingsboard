// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class BlobEntityDataValidator extends DataValidator<BlobEntity> {

    private final TenantService tenantService;
    private final CustomerDao customerDao;

    @Override
    protected BlobEntity validateUpdate(TenantId tenantId, BlobEntity blobEntity) {
        throw new DataValidationException("Update of BlobEntity is prohibited!");
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, BlobEntity blobEntity) {
        if (StringUtils.isEmpty(blobEntity.getType())) {
            throw new DataValidationException("BlobEntity type should be specified!");
        }
        if (StringUtils.isEmpty(blobEntity.getName())) {
            throw new DataValidationException("BlobEntity name should be specified!");
        }
        if (StringUtils.isEmpty(blobEntity.getContentType())) {
            throw new DataValidationException("BlobEntity content type should be specified!");
        }
        if (blobEntity.getData() == null) {
            throw new DataValidationException("BlobEntity data should be specified!");
        }
        if (blobEntity.getTenantId() == null) {
            throw new DataValidationException("BlobEntity should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(blobEntity.getTenantId())) {
                throw new DataValidationException("BlobEntity is referencing to non-existent tenant!");
            }
        }
        if (blobEntity.getCustomerId() == null) {
            blobEntity.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!blobEntity.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, blobEntity.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign blobEntity to non-existent customer!");
            }
            if (!customer.getTenantId().equals(blobEntity.getTenantId())) {
                throw new DataValidationException("Can't assign blobEntity to customer from different tenant!");
            }
        }
    }
}
