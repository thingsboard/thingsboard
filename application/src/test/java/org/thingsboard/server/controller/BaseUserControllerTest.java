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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.mail.TestMailService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseUserControllerTest extends AbstractControllerTest {

    private IdComparator<User> idComparator = new IdComparator<>();

    @Test
    public void testSaveUser() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");
        User savedUser = doPost("/api/user", user, User.class);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());

        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertEquals(foundUser, savedUser);

        logout();
        doGet("/api/noauth/activate?activateToken={activateToken}", TestMailService.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + TestMailService.currentActivateToken));

        JsonNode activateRequest = new ObjectMapper().createObjectNode()
                .put("activateToken", TestMailService.currentActivateToken)
                .put("password", "testPassword");

        JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        logout();

        login(email, "testPassword");

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateUserFromDifferentTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        loginDifferentTenant();
        doPost("/api/user", tenantAdmin, User.class, status().isForbidden());
        deleteDifferentTenant();

        loginSysAdmin();
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testResetPassword() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = createUserAndLogin(user, "testPassword1");
        logout();

        JsonNode resetPasswordByEmailRequest = new ObjectMapper().createObjectNode()
                .put("email", email);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", TestMailService.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + TestMailService.currentResetPasswordToken));

        JsonNode resetPasswordRequest = new ObjectMapper().createObjectNode()
                .put("resetToken", TestMailService.currentResetPasswordToken)
                .put("password", "testPassword2");

        JsonNode tokenInfo = readResponse(doPost("/api/noauth/resetPassword", resetPasswordRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        logout();

        login(email, "testPassword2");
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindUserById() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(savedUser, foundUser);

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveUserWithSameEmail() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = TENANT_ADMIN_EMAIL;
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("User with email '" + email + "'  already present in database")));

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveUserWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = "tenant_thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid email address format '" + email + "'")));

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveUserWithEmptyEmail() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setFirstName("Joe");
        user.setLastName("Downs");

        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("User email should be specified")));

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveUserWithoutTenant() throws Exception {
        loginSysAdmin();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenant2@thingsboard.org");
        user.setFirstName("Joe");
        user.setLastName("Downs");

        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant administrator should be assigned to tenant")));
    }

    @Test
    public void testDeleteUser() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedTenant.getId());
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isNotFound());

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantAdmins() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();

        List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        Assert.assertEquals(tenantAdmins, loadedTenantAdmins);

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());
        
        pageLink = new PageLink(33);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?", 
                new TypeReference<PageData<User>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindTenantAdminsByEmail() throws Exception {

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();

        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(doPost("/api/user", user, User.class));
        }

        String email2 = "testEmail2";
        List<User> tenantAdminsEmail2 = new ArrayList<>();

        for (int i = 0; i < 112; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
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
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        for (User user : loadedTenantAdminsEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?", 
                new TypeReference<PageData<User>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedTenantAdminsEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?", 
                new TypeReference<PageData<User>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindCustomerUsers() throws Exception {

        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        CustomerId customerId = savedCustomer.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 56; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindCustomerUsersByEmail() throws Exception {

        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        CustomerId customerId = savedCustomer.getId();

        String email1 = "testEmail1";
        List<User> customerUsersEmail1 = new ArrayList<>();

        for (int i = 0; i < 74; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail1.add(doPost("/api/user", user, User.class));
        }

        String email2 = "testEmail2";
        List<User> customerUsersEmail2 = new ArrayList<>();

        for (int i = 0; i < 92; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
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
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<PageData<User>>(){}, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (User user : loadedCustomerUsersEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?", 
                new TypeReference<PageData<User>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?", 
                new TypeReference<PageData<User>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

}
