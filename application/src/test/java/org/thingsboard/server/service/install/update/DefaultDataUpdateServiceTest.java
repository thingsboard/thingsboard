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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.when;

@ActiveProfiles("install")
@SpringBootTest(classes = DefaultDataUpdateService.class)
class DefaultDataUpdateServiceTest {

    public static final int CUSTOMER_COUNT = 10;
    ObjectMapper mapper = new ObjectMapper();

    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @MockBean
    DefaultDataUpdateService service;

    @BeforeEach
    void setUp() {
        willCallRealMethod().given(service).convertDeviceProfileAlarmRulesForVersion330(any());
        willCallRealMethod().given(service).convertDeviceProfileForVersion330(any());
        willCallRealMethod().given(service).updateDuplicateCustomersTitle(any());
        willCallRealMethod().given(service).sortCustomersByTitleAndCreatedTime(any());
    }

    JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330FirstRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_in.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isTrue();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330SecondRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_out.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330EmptyJson() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ }");
        JsonNode expected = mapper.readTree("{ }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330AlarmNodeNull() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ \"alarms\" : null }");
        JsonNode expected = mapper.readTree("{ \"alarms\" : null }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330NoAlarmNode() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ \"configuration\": { \"type\": \"DEFAULT\" } }");
        JsonNode expected = mapper.readTree("{ \"configuration\": { \"type\": \"DEFAULT\" } }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void testUpdateDuplicateCustomersTitleWhereAllCustomersHaveEqualsTitle() {
        List<Customer> customers = new ArrayList<>();
        List<Customer> resultCustomers = new ArrayList<>();
        for (int i = 0; i < CUSTOMER_COUNT; i++) {

            // Set created time "CUSTOMER_COUNT - i", for mock. For check sort function before call main tested method we shuffle customers
            Customer customer = createCustomer("Customer A", tenantId, CUSTOMER_COUNT - i);
            customers.add(customer);
            Customer updatedCustomer = getCopyCustomer(customer);
            if (i > 0) {
                updatedCustomer.setTitle(updatedCustomer.getTitle() + "-" + i);
                when(service.updateCustomerTitle(customer.getTenantId(), updatedCustomer)).thenReturn(updatedCustomer);
            }
            resultCustomers.add(updatedCustomer);
        }
        testUpdateCustomerMethod(customers, resultCustomers);
    }

    @Test
    void testUpdateDuplicateCustomersTitleWhereAllCustomersContainsTwoDifferentTitle() {
        List<Customer> customers = new ArrayList<>();
        List<Customer> resultCustomers = new ArrayList<>();
        for (int i = 0; i< CUSTOMER_COUNT; i++) {

            // Set created time "CUSTOMER_COUNT - i", for mock. For check sort function before call main tested method we shuffle customers
            Customer customer = createCustomer("Customer " + (i % 2 == 0 ? "A" : "B"), tenantId, CUSTOMER_COUNT - i);
            customers.add(customer);
            Customer updatedCustomer = getCopyCustomer(customer);
            if (i > 1) {
                updatedCustomer.setTitle(updatedCustomer.getTitle() + "-" + (i / 2));
                when(service.updateCustomerTitle(customer.getTenantId(), updatedCustomer)).thenReturn(updatedCustomer);
            }
            resultCustomers.add(updatedCustomer);
        }
        testUpdateCustomerMethod(customers, resultCustomers);
    }

    void testUpdateCustomerMethod(List<Customer> customers, List<Customer> resultCustomers) {
        service.sortCustomersByTitleAndCreatedTime(resultCustomers);

        // Shuffle customers for check sort function
        Collections.shuffle(customers);

        customers = service.updateDuplicateCustomersTitle(customers);
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
