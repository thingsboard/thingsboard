/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseUserServiceTest extends AbstractServiceTest {

    private IdComparator<User> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant@thingsboard.org");
        userService.saveUser(tenantAdmin);

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer@thingsboard.org");
        userService.saveUser(customerUser);
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindUserByEmail() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.SYS_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.TENANT_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.CUSTOMER_USER, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "fake@thingsboard.org");
        Assert.assertNull(user);
    }

    @Test
    public void testFindUserById() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        User foundUser = userService.findUserById(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser);
    }

    @Test
    public void testFindUserCredentials() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID,"sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(userCredentials);
    }

    @Test
    public void testSaveUser() {
        User tenantAdminUser = userService.findUserByEmail(SYSTEM_TENANT_ID,"tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());
        Assert.assertEquals(user.getTenantId(), savedUser.getTenantId());
        Assert.assertEquals(user.getAuthority(), savedUser.getAuthority());
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        Assert.assertNotNull(userCredentials);
        Assert.assertNotNull(userCredentials.getId());
        Assert.assertNotNull(userCredentials.getUserId());
        Assert.assertNotNull(userCredentials.getActivateToken());

        savedUser.setFirstName("Joe");
        savedUser.setLastName("Downs");

        userService.saveUser(savedUser);
        savedUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertEquals("Joe", savedUser.getFirstName());
        Assert.assertEquals("Downs", savedUser.getLastName());

        userService.deleteUser(tenantId, savedUser.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveUserWithSameEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("sysadmin@thingsboard.org");
        userService.saveUser(tenantAdminUser);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveUserWithInvalidEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("tenant_thingsboard.org");
        userService.saveUser(tenantAdminUser);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveUserWithEmptyEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail(null);
        userService.saveUser(tenantAdminUser);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveUserWithoutTenant() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setTenantId(null);
        userService.saveUser(tenantAdminUser);
    }

    @Test
    public void testDeleteUser() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        User foundUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertNotNull(foundUser);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        Assert.assertNotNull(userCredentials);
        userService.deleteUser(tenantId, foundUser.getId());
        userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        foundUser = userService.findUserById(tenantId, foundUser.getId());
        Assert.assertNull(foundUser);
        Assert.assertNull(userCredentials);
    }

    @Test
    public void testFindTenantAdmins() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        PageData<User> pageData = userService.findTenantAdmins(tenantAdminUser.getTenantId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(tenantAdminUser, users.get(0));

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(userService.saveUser(user));
        }

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        Assert.assertEquals(tenantAdmins, loadedTenantAdmins);

        tenantService.deleteTenant(tenantId);

        pageLink = new PageLink(33);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindTenantAdminsByEmail() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        for (int i = 0; i < 94; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(userService.saveUser(user));
        }

        String email2 = "testEmail2";
        List<User> tenantAdminsEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(userService.saveUser(user));
        }

        List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail1, idComparator);
        Collections.sort(loadedTenantAdminsEmail1, idComparator);

        Assert.assertEquals(tenantAdminsEmail1, loadedTenantAdminsEmail1);

        List<User> loadedTenantAdminsEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        for (User user : loadedTenantAdminsEmail1) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedTenantAdminsEmail2) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindCustomerUsers() {
        User customerUser = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        PageData<User> pageData = userService.findCustomerUsers(customerUser.getTenantId(),
                customerUser.getCustomerId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(customerUser, users.get(0));

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 156; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(userService.saveUser(user));
        }

        List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        tenantService.deleteTenant(tenantId);

        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindCustomerUsersByEmail() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        String email1 = "testEmail1";
        List<User> customerUsersEmail1 = new ArrayList<>();

        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail1.add(userService.saveUser(user));
        }

        String email2 = "testEmail2";
        List<User> customerUsersEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(userService.saveUser(user));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail1, idComparator);
        Collections.sort(loadedCustomerUsersEmail1, idComparator);

        Assert.assertEquals(customerUsersEmail1, loadedCustomerUsersEmail1);

        List<User> loadedCustomerUsersEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (User user : loadedCustomerUsersEmail1) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        tenantService.deleteTenant(tenantId);
    }

}
