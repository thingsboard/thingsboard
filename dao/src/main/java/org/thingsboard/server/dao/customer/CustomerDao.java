// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.customer;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

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

}
