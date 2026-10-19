// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaCustomerDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private CustomerDao customerDao;

    @Test
    public void testFindByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createCustomer(tenantId1, i);
            createCustomer(tenantId2, i * 2);
        }

        PageLink pageLink = new PageLink(15, 0,  "CUSTOMER");
        PageData<Customer> customers1 = customerDao.findCustomersByTenantId(tenantId1, pageLink);
        assertEquals(15, customers1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Customer> customers2 = customerDao.findCustomersByTenantId(tenantId1, pageLink);
        assertEquals(5, customers2.getData().size());
    }

    @Test
    public void testFindCustomersByTenantIdAndTitle() {
        UUID tenantId = Uuids.timeBased();

        for (int i = 0; i < 10; i++) {
            createCustomer(tenantId, i);
        }

        Optional<Customer> customerOpt = customerDao.findCustomerByTenantIdAndTitle(tenantId, "CUSTOMER_5");
        assertTrue(customerOpt.isPresent());
        assertEquals("CUSTOMER_5", customerOpt.get().getTitle());
    }

    @Test
    public void testFindPublicCustomerByTenantId() {
        UUID tenantUUID = Uuids.timeBased();

        Optional<Customer> customerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantUUID, tenantUUID);
        assertTrue(customerOpt.isEmpty());

        String publicCustomerTitle = StringUtils.randomAlphanumeric(10);
        createPublicCustomer(TenantId.fromUUID(tenantUUID), publicCustomerTitle);
        customerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantUUID, tenantUUID);
        assertTrue(customerOpt.isPresent());
        Customer customer = customerOpt.get();
        assertTrue(customer.isPublic());
        assertEquals(publicCustomerTitle, customer.getTitle());
    }

    @Test
    public void testFindPublicCustomerByTenantIdAndParentCustomerId() {
        UUID tenantUUID = Uuids.timeBased();
        String parentCustomerTitle = "CUSTOMER_0";

        createCustomer(tenantUUID, 0);

        Optional<Customer> customerOpt = customerDao.findCustomerByTenantIdAndTitle(tenantUUID, parentCustomerTitle);
        assertTrue(customerOpt.isPresent());
        Customer parentCustomer = customerOpt.get();
        assertEquals(parentCustomerTitle, parentCustomer.getTitle());
        CustomerId parentCustomerId = parentCustomer.getId();

        String publicCustomerTitle = StringUtils.randomAlphanumeric(10);
        createPublicCustomer(TenantId.fromUUID(tenantUUID), parentCustomerId, publicCustomerTitle);
        customerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantUUID, parentCustomerId.getId());
        assertTrue(customerOpt.isPresent());
        Customer customer = customerOpt.get();
        assertTrue(customer.isPublic());
        assertEquals(publicCustomerTitle, customer.getTitle());
        assertEquals(parentCustomerId, customer.getParentCustomerId());
    }

    private void createCustomer(UUID tenantId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

    private void createPublicCustomer(TenantId tenantId, String publicCustomerTitle) {
        createPublicCustomer(tenantId, null, publicCustomerTitle);
    }

    private void createPublicCustomer(TenantId tenantId, EntityId ownerId, String publicCustomerTitle) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        customer.setTenantId(tenantId);
        if (ownerId != null) {
            customer.setOwnerId(ownerId);
        }
        customer.setTitle(publicCustomerTitle);
        customer.setAdditionalInfo(CustomerServiceImpl.PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON);
        customerDao.save(tenantId, customer);
    }

}
