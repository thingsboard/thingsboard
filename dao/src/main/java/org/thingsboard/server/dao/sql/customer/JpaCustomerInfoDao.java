// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerInfoDao;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.model.sql.CustomerInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaCustomerInfoDao extends JpaAbstractDao<CustomerInfoEntity, CustomerInfo> implements CustomerInfoDao {

    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @Override
    protected Class<CustomerInfoEntity> getEntityClass() {
        return CustomerInfoEntity.class;
    }

    @Override
    protected JpaRepository<CustomerInfoEntity, UUID> getRepository() {
        return customerInfoRepository;
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findTenantCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findTenantCustomersByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

}
