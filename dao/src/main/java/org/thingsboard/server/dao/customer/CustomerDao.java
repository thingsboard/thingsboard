/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface CustomerDao.
 */
public interface CustomerDao extends Dao<Customer>, TenantEntityDao<Customer>, ExportableEntityDao<CustomerId, Customer> {

    /**
     * Save or update customer object
     *
     * @param customer the customer object
     * @return saved customer object
     */
    Customer save(TenantId tenantId, Customer customer);

    /**
     * Find customers by tenant id and page link.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the page of customer objects
     */
    PageData<Customer> findCustomersByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find customer by tenantId and customer title.
     *
     * @param tenantId the tenantId
     * @param title the customer title
     * @return the optional customer object
     */
    Optional<Customer> findCustomerByTenantIdAndTitle(UUID tenantId, String title);

    /**
     * Find public customer by tenantId.
     *
     * @param tenantId the tenantId
     * @return the optional public customer object
     */
    Optional<Customer> findPublicCustomerByTenantId(UUID tenantId);


    /**
     * Find customers with the same title within the same tenant.
     * This method was created to upgrade customers with the same title before creation of
     * CONSTRAINT customer_title_unq_key UNIQUE (tenant_id, title).
     * If constraint already exists this method will return nothing.
     *
     * @param pageLink the page link
     * @return the page of customer objects
     */
    PageData<Customer> findCustomersWithTheSameTitle(PageLink pageLink);

    List<Customer> findCustomersByTenantIdAndIds(UUID tenantId, List<UUID> customerIds);

}
