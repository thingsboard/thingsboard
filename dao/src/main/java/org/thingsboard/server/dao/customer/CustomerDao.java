// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface CustomerDao.
 */
public interface CustomerDao extends Dao<Customer>, TenantEntityDao<Customer>, ExportableCustomerEntityDao<Customer, CustomerId> {

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
     * Find public customer by tenantId and ownerId.
     *
     * @param tenantId the tenantId
     * @param ownerId the ownerId
     * @return the optional public customer object
     */
    Optional<Customer> findPublicCustomerByTenantIdAndOwnerId(UUID tenantId, UUID ownerId);

    /**
     * Find customers by tenantId and customer Ids.
     *
     * @param tenantId the tenantId
     * @param customerIds the customer Ids
     * @return the list of customer objects
     */
    ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> customerIds);

    List<Customer> findCustomersByTenantIdAndIds(UUID tenantId, List<UUID> customerIds);

    PageData<Customer> findCustomersByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<Customer> findCustomersByEntityGroupIds(List<UUID> groupIds, List<UUID> additionalCustomerIds, PageLink pageLink);


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

    List<Customer> findCustomersByCustomMenuId(CustomMenuId id);

    void updateCustomersCustomMenuId(List<CustomerId> customerIds, CustomMenuId customMenuId);

    PageData<Customer> findByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink);

}
