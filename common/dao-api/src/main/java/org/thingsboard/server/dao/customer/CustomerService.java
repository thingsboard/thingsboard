/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.Optional;

public interface CustomerService {

    Customer findCustomerById(TenantId tenantId, CustomerId customerId);

    Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title);

    ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId);

    Customer saveCustomer(Customer customer);

    void deleteCustomer(TenantId tenantId, CustomerId customerId);

    Customer findOrCreatePublicCustomer(TenantId tenantId);

    TextPageData<Customer> findCustomersByTenantId(TenantId tenantId, TextPageLink pageLink);

    void deleteCustomersByTenantId(TenantId tenantId);

}
