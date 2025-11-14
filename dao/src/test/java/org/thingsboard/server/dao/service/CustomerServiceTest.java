/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class CustomerServiceTest extends AbstractServiceTest {

    @Autowired
    CustomerService customerService;
    @Autowired
    CalculatedFieldService calculatedFieldService;
    @Autowired
    AssetService assetService;

    static final int TIMEOUT = 30;

    ListeningExecutorService executor;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void after() {
        executor.shutdownNow();
    }

    @Test
    public void testSaveCustomer() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());


        savedCustomer.setTitle("My new customer");

        customerService.saveCustomer(savedCustomer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertEquals(foundCustomer.getTitle(), savedCustomer.getTitle());

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testFindCustomerById() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNotNull(foundCustomer);
        Assert.assertEquals(savedCustomer, foundCustomer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testSaveCustomerWithEmptyTitle() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    @Test
    public void testSaveCustomerWithEmptyTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    @Test
    public void testSaveCustomerWithInvalidTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    @Test
    public void testSaveCustomerWithInvalidEmail() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customer.setEmail("invalid@mail");
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    @Test
    public void testDeleteCustomer() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNull(foundCustomer);
    }

    @Test
    public void testFindCustomersByTenantId() throws Exception {

        List<ListenableFuture<Customer>> futures = new ArrayList<>(135);
        for (int i = 0; i < 135; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle("Customer" + i);
            futures.add(executor.submit(() ->
                    customerService.saveCustomer(customer)));
        }
        List<Customer> customers = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Customer> loadedCustomers = new ArrayList<>(135);
        PageLink pageLink = new PageLink(23);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customers).containsExactlyInAnyOrderElementsOf(loadedCustomers);

        customerService.deleteCustomersByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindCustomersByTenantIdAndTitleAsTextSearch() throws Exception {
        String title1 = "Customer title 1";
        List<ListenableFuture<Customer>> futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
        }
        List<Customer> customersTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Customer title 2";
        futures = new ArrayList<>(175);
        for (int i = 0; i < 175; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
        }
        List<Customer> customersTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Customer> loadedCustomersTitle1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle1);

        List<Customer> loadedCustomersTitle2 = new ArrayList<>(175);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle2);

        List<ListenableFuture<Void>> deleteFutures = new ArrayList<>(143);
        for (Customer customer : loadedCustomersTitle1) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteFutures = new ArrayList<>(175);
        for (Customer customer : loadedCustomersTitle2) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerByTitle() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());

        Optional<Customer> foundCustomerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, savedCustomer.getTitle());
        Assert.assertTrue(foundCustomerOpt.isPresent());
        Assert.assertEquals(foundCustomerOpt.get().getTitle(), savedCustomer.getTitle());

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testSaveCustomerWithExistingTitle() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());

        assertThatThrownBy(() -> customerService.saveCustomer(customer))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Customer with such title already exists!");

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testFindOrCreatePublicCustomer_Concurrency() throws Exception {
        CountDownLatch allThreadsReadyLatch = new CountDownLatch(2);
        final Customer[] customers = new Customer[2];

        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(2);
            for (int i = 0; i < 2; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    allThreadsReadyLatch.countDown();
                    try {
                        allThreadsReadyLatch.await();
                        customers[threadIndex] = customerService.findOrCreatePublicCustomer(tenantId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            executor.shutdown();

            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(executor::isTerminated);
            Customer firstCustomer = customers[0];
            Customer secondCustomer = customers[1];
            assertThat(firstCustomer).isNotNull();
            assertThat(secondCustomer).isNotNull();
            assertThat(firstCustomer).isEqualTo(secondCustomer);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testDeleteCustomerIfReferencedInCalculatedField() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setName("Test CF");
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setEntityId(savedAsset.getId());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(savedCustomer.getId());
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> customerService.deleteCustomer(tenantId, savedCustomer.getId()))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't delete customer that is referenced in calculated fields!");

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

}
