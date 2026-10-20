// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.CustomerFields;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaCustomerDao extends JpaAbstractDao<CustomerEntity, Customer> implements CustomerDao {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    protected Class<CustomerEntity> getEntityClass() {
        return CustomerEntity.class;
    }

    @Override
    protected JpaRepository<CustomerEntity, UUID> getRepository() {
        return customerRepository;
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository.findByTenantId(
                tenantId,
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(UUID tenantId, String title) {
        return Optional.ofNullable(DaoUtil.getData(customerRepository.findByTenantIdAndTitle(tenantId, title)));
    }

    @Override
    public ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> customerIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(customerIds, ids ->
                customerRepository.findCustomersByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public List<Customer> findCustomersByTenantIdAndIds(UUID tenantId, List<UUID> customerIds) {
        return DaoUtil.convertDataList(customerRepository.findCustomersByTenantIdAndIdIn(tenantId, customerIds));
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository
                .findByEntityGroupId(
                        groupId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupIds(List<UUID> groupIds, List<UUID> additionalCustomerIds, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository
                .findByEntityGroupIds(
                        groupIds,
                        additionalCustomerIds != null && !additionalCustomerIds.isEmpty() ? additionalCustomerIds : null,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public Optional<Customer> findPublicCustomerByTenantIdAndOwnerId(UUID tenantId, UUID ownerId) {
        var customerEntity = tenantId.equals(ownerId) ?
                customerRepository.findPublicCustomerByTenantIdAndNullCustomerId(tenantId) :
                customerRepository.findPublicCustomerByTenantIdAndCustomerId(tenantId, ownerId);
        return Optional.ofNullable(DaoUtil.getData(customerEntity));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return customerRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Customer findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(customerRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Customer findByTenantIdAndName(UUID tenantId, String name) {
        return findCustomerByTenantIdAndTitle(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Customer> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findCustomersByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<CustomerId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = customerRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = customerRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, CustomerId::new);
    }

    @Override
    public CustomerId getExternalIdByInternal(CustomerId internalId) {
        return Optional.ofNullable(customerRepository.getExternalIdById(internalId.getId()))
                .map(CustomerId::new).orElse(null);
    }

    @Override
    public PageData<Customer> findCustomersWithTheSameTitle(PageLink pageLink) {
        return DaoUtil.toPageData(
                customerRepository.findCustomersWithTheSameTitle(DaoUtil.toPageable(pageLink))
        );
    }

    @Override
    public List<Customer> findCustomersByCustomMenuId(CustomMenuId customMenuId) {
        return DaoUtil.convertDataList(customerRepository.findByCustomMenuId(customMenuId.getId()));
    }

    @Override
    public void updateCustomersCustomMenuId(List<CustomerId> customerIds, CustomMenuId customMenuId) {
        if (customMenuId == null) {
            customerRepository.updateCustomMenuIdToNull(toUUIDs(customerIds));
        } else {
            customerRepository.updateCustomMenuId(toUUIDs(customerIds), customMenuId.getId());
        }
    }

    @Override
    public PageData<Customer> findByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink) {
        if (parentCustomerId != null && !parentCustomerId.isNullUid()) {
            return DaoUtil.toPageData(customerRepository.findByTenantIdAndParentCustomerId(tenantId.getId(), parentCustomerId.getId(), DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(customerRepository.findByTenantIdAndNullParentCustomerId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
        }
    }

    @Override
    public PageData<Customer> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<CustomerFields> findNextBatch(UUID id, int batchSize) {
        return customerRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        return customerRepository.findEntityInfosByNamePrefix(tenantId.getId(), name);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
