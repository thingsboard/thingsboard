// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AssignDeviceToCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignDeviceFromCustomerArgs;
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

            Customer createdCustomer = client.saveCustomer(SaveCustomerArgs.builder()
                    .customer(customer)
                    .build());
            assertNotNull(createdCustomer);
            assertNotNull(createdCustomer.getId());
            assertEquals(customerTitle, createdCustomer.getTitle());

            createdCustomers.add(createdCustomer);
        }

        // find all, check count (includes savedClientCustomer from AbstractApiClientTest setup)
        PageDataCustomer allCustomers = client.getCustomers(GetCustomersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allCustomers);
        assertNotNull(allCustomers.getData());
        int initialSize = allCustomers.getData().size();
        assertEquals("Expected 21 customers (20 created + 1 from setup), but got " + initialSize, 21, initialSize);

        // find all with search text, check count
        PageDataCustomer filteredCustomers = client.getCustomers(GetCustomersArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 customers matching prefix", 10, filteredCustomers.getData().size());

        // find by id
        Customer searchCustomer = createdCustomers.get(10);
        Customer fetchedCustomer = client.getCustomerById(GetCustomerByIdArgs.builder()
                .customerId(searchCustomer.getId().getId().toString())
                .build());
        assertEquals(searchCustomer.getTitle(), fetchedCustomer.getTitle());

        // find by title
        Customer fetchedByTitle = client.getTenantCustomer(GetTenantCustomerArgs.builder()
                .customerTitle(searchCustomer.getTitle())
                .build());
        assertEquals(searchCustomer.getId().getId(), fetchedByTitle.getId().getId());

        // update customer
        fetchedCustomer.setCity("New York");
        fetchedCustomer.setCountry("US");
        Customer updatedCustomer = client.saveCustomer(SaveCustomerArgs.builder()
                .customer(fetchedCustomer)
                .build());
        assertEquals("New York", updatedCustomer.getCity());
        assertEquals("US", updatedCustomer.getCountry());

        // assign device to customer and verify
        Device device = new Device();
        device.setName("CustomerTestDevice_" + timestamp);
        device.setType("default");
        Device createdDevice = client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());

        String customerId = createdCustomers.get(0).getId().getId().toString();
        client.assignDeviceToCustomer(AssignDeviceToCustomerArgs.builder()
                .customerId(customerId)
                .deviceId(createdDevice.getId().getId().toString())
                .build());

        PageDataDevice customerDevices = client.getCustomerDevices(GetCustomerDevicesArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(1, customerDevices.getData().size());
        assertEquals(createdDevice.getName(), customerDevices.getData().get(0).getName());

        // unassign device from customer
        client.unassignDeviceFromCustomer(UnassignDeviceFromCustomerArgs.builder()
                .deviceId(createdDevice.getId().getId().toString())
                .build());
        PageDataDevice devicesAfterUnassign = client.getCustomerDevices(GetCustomerDevicesArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(0, devicesAfterUnassign.getData().size());

        // delete customer
        UUID customerToDeleteId = createdCustomers.get(0).getId().getId();
        client.deleteCustomer(DeleteCustomerArgs.builder()
                .customerId(customerToDeleteId.toString())
                .build());

        // verify deletion
        PageDataCustomer customersAfterDelete = client.getCustomers(GetCustomersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize - 1, customersAfterDelete.getData().size());

        assertReturns404(() ->
                client.getCustomerById(GetCustomerByIdArgs.builder()
                        .customerId(customerToDeleteId.toString())
                        .build())
        );
    }

}
