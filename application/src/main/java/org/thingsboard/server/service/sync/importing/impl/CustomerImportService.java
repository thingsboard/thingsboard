/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.data.CustomerExportData;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;
import org.thingsboard.server.utils.ThrowingRunnable;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class CustomerImportService extends BaseEntityImportService<CustomerId, Customer, CustomerExportData> {

    private final CustomerService customerService;

    @Override
    protected void setOwner(TenantId tenantId, Customer customer, NewIdProvider idProvider) {
        customer.setTenantId(tenantId);
    }

    @Override
    protected Customer prepareAndSave(TenantId tenantId, Customer customer, CustomerExportData exportData, NewIdProvider idProvider) {
        return customerService.saveCustomer(customer);
    }

    @Override
    protected ThrowingRunnable getCallback(SecurityUser user, Customer savedCustomer, Customer oldCustomer) {
        return () -> {
            entityActionService.onCustomerCreatedOrUpdated(user, savedCustomer);
        };
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
