/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.PageDataCustomer;
import org.thingsboard.client.model.PageDataDevice;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class CustomerApiClientTest extends AbstractApiClientTest {

    @Test
    public void testCustomerLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Customer> createdCustomers = new ArrayList<>();

        // create 20 customers
        for (int i = 0; i < 20; i++) {
            Customer customer = new Customer();
            String customerTitle = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            customer.setTitle(customerTitle);
            customer.setEmail("customer_" + timestamp + "_" + i + "@test.com");

            Customer createdCustomer = client.saveCustomer(customer, null, null, null);
            assertNotNull(createdCustomer);
            assertNotNull(createdCustomer.getId());
            assertEquals(customerTitle, createdCustomer.getTitle());

            createdCustomers.add(createdCustomer);
        }

        // find all, check count (includes savedClientCustomer from AbstractApiClientTest setup)
        PageDataCustomer allCustomers = client.getCustomers(100, 0, null, null, null);
        assertNotNull(allCustomers);
        assertNotNull(allCustomers.getData());
        int initialSize = allCustomers.getData().size();
        assertEquals("Expected 21 customers (20 created + 1 from setup), but got " + initialSize, 21, initialSize);

        // find all with search text, check count
        PageDataCustomer filteredCustomers = client.getCustomers(100, 0, TEST_PREFIX_2, null, null);
        assertEquals("Expected exactly 10 customers matching prefix", 10, filteredCustomers.getData().size());

        // find by id
        Customer searchCustomer = createdCustomers.get(10);
        Customer fetchedCustomer = client.getCustomerById(searchCustomer.getId().getId().toString());
        assertEquals(searchCustomer.getTitle(), fetchedCustomer.getTitle());

        // find by title
        Customer fetchedByTitle = client.getTenantCustomer(searchCustomer.getTitle());
        assertEquals(searchCustomer.getId().getId(), fetchedByTitle.getId().getId());

        // update customer
        fetchedCustomer.setCity("New York");
        fetchedCustomer.setCountry("US");
        Customer updatedCustomer = client.saveCustomer(fetchedCustomer, null, null, null);
        assertEquals("New York", updatedCustomer.getCity());
        assertEquals("US", updatedCustomer.getCountry());

        // assign device to customer and verify
        Device device = new Device();
        device.setName("CustomerTestDevice_" + timestamp);
        device.setType("default");
        Device createdDevice = client.saveDevice(device, null, null, null, null);

        String customerId = createdCustomers.get(0).getId().getId().toString();
        client.assignDeviceToCustomer(customerId, createdDevice.getId().getId().toString());

        PageDataDevice customerDevices = client.getCustomerDevices(customerId, 100, 0, null, null, null, null);
        assertEquals(1, customerDevices.getData().size());
        assertEquals(createdDevice.getName(), customerDevices.getData().get(0).getName());

        // unassign device from customer
        client.unassignDeviceFromCustomer(createdDevice.getId().getId().toString());
        PageDataDevice devicesAfterUnassign = client.getCustomerDevices(customerId, 100, 0, null, null, null, null);
        assertEquals(0, devicesAfterUnassign.getData().size());

        // delete customer
        UUID customerToDeleteId = createdCustomers.get(0).getId().getId();
        client.deleteCustomer(customerToDeleteId.toString());

        // verify deletion
        PageDataCustomer customersAfterDelete = client.getCustomers(100, 0, null, null, null);
        assertEquals(initialSize - 1, customersAfterDelete.getData().size());

        assertReturns404(() ->
                client.getCustomerById(customerToDeleteId.toString())
        );
    }

}
