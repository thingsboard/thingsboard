// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerService extends EntityDaoService {

    Customer findCustomerById(TenantId tenantId, CustomerId customerId);

    CustomerInfo findCustomerInfoById(TenantId tenantId, CustomerId customerId);

    Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title);

    ListenableFuture<Optional<Customer>> findCustomerByTenantIdAndTitleAsync(TenantId tenantId, String title);

    ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(TenantId tenantId, List<CustomerId> customerIds);

    List<Customer> findCustomersByTenantIdAndIds(TenantId tenantId, List<CustomerId> customerIds);

    Customer saveCustomer(Customer customer);

    Customer saveCustomer(Customer customer, NameConflictStrategy nameConflictStrategy);

    void deleteCustomer(TenantId tenantId, CustomerId customerId);

    Customer findOrCreatePublicCustomer(TenantId tenantId, EntityId ownerId);

    Customer findPublicCustomer(TenantId tenantId, EntityId ownerId);

    EntityGroup findOrCreatePublicUserGroup(TenantId tenantId, EntityId ownerId);

    Role findOrCreatePublicUserEntityGroupRole(TenantId tenantId, EntityId ownerId);

    PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteCustomersByTenantId(TenantId tenantId);

    PageData<Customer> findCustomersByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<Customer> findCustomersByEntityGroupIds(List<EntityGroupId> groupIds, List<CustomerId> additionalCustomerIds, PageLink pageLink);

    PageData<CustomerInfo> findCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CustomerInfo> findTenantCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Customer> findCustomersByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink);

    PageData<CustomerId> findCustomerIdsByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink);

    PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    List<Customer> findCustomersByCustomMenuId(CustomMenuId customMenuId);

    void updateCustomersCustomMenuId(List<CustomerId> customerIds, CustomMenuId customMenuId);

}
