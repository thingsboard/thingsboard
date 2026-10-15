// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AddEntitiesToEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllCustomerInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerCustomerInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomersByEntityGroupIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomersByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserCustomersArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.PageDataCustomer;
import org.thingsboard.client.model.PageDataCustomerInfo;
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

        PageDataCustomer allCustomers = client.getCustomers(GetCustomersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allCustomers);
        assertNotNull(allCustomers.getData());
        int initialSize = allCustomers.getData().size();
        assertEquals("Expected 22 customers (20 created + 2 from setup), but got " + initialSize, 22, initialSize);

        PageDataCustomer filteredCustomers = client.getCustomers(GetCustomersArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 customers matching prefix", 10, filteredCustomers.getData().size());

        Customer searchCustomer = createdCustomers.get(10);
        Customer fetchedCustomer = client.getCustomerById(GetCustomerByIdArgs.builder()
                .customerId(searchCustomer.getId().getId().toString())
                .build());
        assertEquals(searchCustomer.getTitle(), fetchedCustomer.getTitle());

        Customer fetchedByTitle = client.getTenantCustomer(GetTenantCustomerArgs.builder()
                .customerTitle(searchCustomer.getTitle())
                .build());
        assertEquals(searchCustomer.getId().getId(), fetchedByTitle.getId().getId());

        fetchedCustomer.setCity("New York");
        fetchedCustomer.setCountry("US");
        Customer updatedCustomer = client.saveCustomer(SaveCustomerArgs.builder()
                .customer(fetchedCustomer)
                .build());
        assertEquals("New York", updatedCustomer.getCity());
        assertEquals("US", updatedCustomer.getCountry());

        PageDataCustomer userCustomers = client.getUserCustomers(GetUserCustomersArgs.builder()
                .pageSize("100")
                .page("0")
                .build());
        assertEquals("Expected 22 customers (20 created + 2 from setup), but got " + userCustomers.getTotalElements(),
                Long.valueOf(22), userCustomers.getTotalElements());

        PageDataCustomerInfo allCustomerInfos = client.getAllCustomerInfos(GetAllCustomerInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertEquals("Expected 22 customers (20 created + 2 from setup), but got " + allCustomerInfos.getTotalElements(),
                Long.valueOf(22), allCustomerInfos.getTotalElements());

        UUID customerToDeleteId = createdCustomers.get(0).getId().getId();
        client.deleteCustomer(DeleteCustomerArgs.builder()
                .customerId(customerToDeleteId.toString())
                .build());

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

    @Test
    public void testGetCustomerCustomerInfos() throws Exception {
        String customerId = savedClientCustomer.getId().getId().toString();

        PageDataCustomerInfo result = client.getCustomerCustomerInfos(GetCustomerCustomerInfosArgs.builder()
                .customerId(customerId)
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    public void testGetCustomersByIdsV2() throws Exception {
        String customerId = savedClientCustomer.getId().getId().toString();

        List<Customer> result = client.getCustomersByIds(GetCustomersByIdsArgs.builder()
                .customerIds(List.of(customerId))
                .build());
        assertNotNull(result);
        assertEquals("Expected exactly one customer returned", 1, result.size());
        assertEquals(savedClientCustomer.getId().getId(), result.get(0).getId().getId());
    }

    @Test
    public void testGetCustomersByEntityGroupId() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityGroup.TypeEnum.CUSTOMER);
        entityGroup.setName("Test Customer Group");
        EntityGroupInfo savedGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(entityGroup)
                .build());
        assertNotNull(savedGroup);
        assertNotNull(savedGroup.getId());

        String groupId = savedGroup.getId().getId().toString();
        String customerId = savedClientCustomer.getId().getId().toString();

        client.addEntitiesToEntityGroup(AddEntitiesToEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .requestBody(List.of(customerId))
                .build());

        PageDataCustomer result = client.getCustomersByEntityGroupId(GetCustomersByEntityGroupIdArgs.builder()
                .entityGroupId(groupId)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals("Expected exactly one customer in the entity group", 1, result.getData().size());
        assertEquals(savedClientCustomer.getId().getId(), result.getData().get(0).getId().getId());
    }

}
