// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerInfoDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaCustomerInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private CustomerInfoDao customerInfoDao;

    @Autowired
    private CustomerDao customerDao;

    private List<Customer> customers = new ArrayList<>();

    @After
    public void tearDown() {
        for (Customer customer : customers) {
            customerDao.removeById(customer.getTenantId(), customer.getUuidId());
        }
        customers.clear();
    }

    @Test
    public void testFindCustomerInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            customers.add(createCustomer(tenantId1, null, i));
            customers.add(createCustomer(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "CUSTOMER");
        PageData<CustomerInfo> customerInfos1 = customerInfoDao.findCustomersByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, customerInfos1.getData().size());

        PageData<CustomerInfo> customerInfos2 = customerInfoDao.findCustomersByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, customerInfos2.getData().size());
    }

    @Test
    public void testFindCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer("PARENT_", tenantId1, null, 0);
        customers.add(customer1);
        Customer subCustomer2 = createCustomer("PARENT_", tenantId1, customer1.getUuidId(),1);
        customers.add(subCustomer2);

        for (int i = 2; i < 22; i++) {
            customers.add(createCustomer(tenantId1, customer1.getUuidId(), i));
            customers.add(createCustomer(tenantId1, subCustomer2.getUuidId(), 22 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "CUSTOMER", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<CustomerInfo> customerInfos1 = customerInfoDao.findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, customerInfos1.getData().size());
        customerInfos1.getData().forEach(customerInfo -> Assert.assertNotEquals("CUSTOMER_0", customerInfo.getOwnerName()));

        PageData<CustomerInfo> customerInfos2 = customerInfoDao.findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, customerInfos2.getData().size());

        PageData<CustomerInfo> customerInfos3 = customerInfoDao.findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, customerInfos3.getData().size());
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        return this.createCustomer("CUSTOMER_", tenantId, parentCustomerId, index);
    }

    private Customer createCustomer(String customerNamePrefix, UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle(customerNamePrefix + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

}
