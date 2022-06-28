/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.install.update;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TenantCustomersTitleUpdaterComponent.class)
public class TenantCustomersTitleUpdaterComponentTest {

    public static final int CUSTOMER_COUNT = 10;
    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    TenantCustomersTitleUpdaterComponent tenantCustomersTitleUpdaterComponent = mock(TenantCustomersTitleUpdaterComponent.class);

    @Before
    public void setUp() {
        willCallRealMethod().given(tenantCustomersTitleUpdaterComponent).updateDuplicateCustomersTitle(anyList());
    }

    @Test
    public void testUpdateDuplicateCustomersTitleWhereAllCustomersHaveEqualsTitle() {
        List<Customer> customers = new ArrayList<>();
        List<Customer> resultCustomers = new ArrayList<>();
        for (int i = 0; i < CUSTOMER_COUNT; i++) {

            // Set created time "CUSTOMER_COUNT - i", for mock. For check sort function before call main tested method we shuffle customers
            Customer customer = createCustomer("Customer A", tenantId, CUSTOMER_COUNT - i);
            customers.add(customer);
            Customer updatedCustomer = getCopyCustomer(customer);
            if (i > 0) {
                updatedCustomer.setTitle(updatedCustomer.getTitle() + "-" + i);
                when(tenantCustomersTitleUpdaterComponent.updateCustomerTitle(customer.getTenantId(), updatedCustomer)).thenReturn(updatedCustomer);
            }
            resultCustomers.add(updatedCustomer);
        }
        testUpdateCustomerMethod(customers, resultCustomers);
    }

    @Test
    public void testUpdateDuplicateCustomersTitleWhereAllCustomersContainsTwoDifferentTitle() {
        List<Customer> customers = new ArrayList<>();
        List<Customer> resultCustomers = new ArrayList<>();
        for (int i = 0; i< CUSTOMER_COUNT; i++) {

            // Set created time "CUSTOMER_COUNT - i", for mock. For check sort function before call main tested method we shuffle customers
            Customer customer = createCustomer("Customer " + (i % 2 == 0 ? "A" : "B"), tenantId, CUSTOMER_COUNT - i);
            customers.add(customer);
            Customer updatedCustomer = getCopyCustomer(customer);
            if (i > 1) {
                updatedCustomer.setTitle(updatedCustomer.getTitle() + "-" + (i / 2));
                when(tenantCustomersTitleUpdaterComponent.updateCustomerTitle(customer.getTenantId(), updatedCustomer)).thenReturn(updatedCustomer);
            }
            resultCustomers.add(updatedCustomer);
        }
        customers.sort(Comparator.comparing(Customer::getTitle));
        resultCustomers.sort(Comparator.comparing(Customer::getTitle));
        testUpdateCustomerMethod(customers, resultCustomers);
    }

    void testUpdateCustomerMethod(List<Customer> customers, List<Customer> resultCustomers) {
        customers = tenantCustomersTitleUpdaterComponent.updateDuplicateCustomersTitle(customers);
        Assertions.assertEquals(customers, resultCustomers);
    }

    @NotNull
    Customer getCopyCustomer(Customer customer) {
        Customer updatedCustomer = new Customer();
        updatedCustomer.setTitle(customer.getTitle());
        updatedCustomer.setTenantId(customer.getTenantId());
        updatedCustomer.setCreatedTime(customer.getCreatedTime());
        return updatedCustomer;
    }

    Customer createCustomer(String title, TenantId tenantId, int createdTime) {
        Customer customer = new Customer();
        customer.setTitle(title);
        customer.setTenantId(tenantId);
        customer.setCreatedTime(createdTime);
        return customer;
    }
}
