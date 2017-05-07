package org.thingsboard.server.dao.sql.customer;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaCustomerDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private CustomerDao customerDao;

    @Test
    public void testFindByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();

        for (int i = 0; i < 20; i++) {
            createCustomer(tenantId1, i);
            createCustomer(tenantId2, i * 2);
        }

        TextPageLink pageLink1 = new TextPageLink(15, "CUSTOMER");
        List<Customer> customers1 = customerDao.findCustomersByTenantId(tenantId1, pageLink1);
        assertEquals(15, customers1.size());

        TextPageLink pageLink2 = new TextPageLink(15, "CUSTOMER", customers1.get(14).getId().getId(), null);
        List<Customer> customers2 = customerDao.findCustomersByTenantId(tenantId1, pageLink2);
        assertEquals(5, customers2.size());
    }

    private void createCustomer(UUID tenantId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(UUIDs.timeBased()));
        customer.setTenantId(new TenantId(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        customerDao.save(customer);
    }
}
