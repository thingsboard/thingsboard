/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserData;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.service.mail.TestMailService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.SYSTEM_TENANT;

@ContextConfiguration(classes = {BaseUserControllerTest.Config.class})
public abstract class BaseUserControllerTest extends AbstractControllerTest {

    private IdComparator<User> idComparator = new IdComparator<>();
    private IdComparator<UserData> userDataIdComparator = new IdComparator<>();

    private CustomerId customerNUULId = (CustomerId) createEntityId_NULL_UUID(new Customer());

    @Autowired
    private UserDao userDao;

    static class Config {
        @Bean
        @Primary
        public UserDao userDao(UserDao userDao) {
            return Mockito.mock(UserDao.class, AdditionalAnswers.delegatesTo(userDao));
        }
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
    }

    @Test
    public void testSaveUser() throws Exception {
        loginSysAdmin();

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        Mockito.reset(tbClusterService, auditLogService);

        User savedUser = doPost("/api/user", user, User.class);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());

        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertEquals(foundUser, savedUser);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundUser, foundUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, ActionType.ADDED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        resetTokens();
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

        resetTokens();

        login(email, "testPassword");

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(foundUser, foundUser.getId(), foundUser.getId(),
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, SYSTEM_TENANT.getId().toString());
    }

    @Test
    public void testSaveUserWithViolationOfFiledValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName(StringUtils.randomAlphabetic(300));
        user.setLastName("Downs");
        String msgError = msgErrorFieldLength("first name");
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        user.setFirstName("Normal name");
        msgError = msgErrorFieldLength("last name");
        user.setLastName(StringUtils.randomAlphabetic(300));
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateUserFromDifferentTenant() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/user", tenantAdmin)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(tenantAdmin.getId(), tenantAdmin);

        deleteDifferentTenant();
    }

    @Test
    public void testResetPassword() throws Exception {
        loginSysAdmin();

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = createUserAndLogin(user, "testPassword1");
        resetTokens();

        JsonNode resetPasswordByEmailRequest = new ObjectMapper().createObjectNode()
                .put("email", email);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", TestMailService.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + TestMailService.currentResetPasswordToken));

        JsonNode resetPasswordRequest = new ObjectMapper().createObjectNode()
                .put("resetToken", TestMailService.currentResetPasswordToken)
                .put("password", "testPassword2");

        JsonNode tokenInfo = readResponse(
                doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        resetTokens();

        login(email, "testPassword2");
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindUserById() throws Exception {
        loginSysAdmin();

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(savedUser, foundUser);
    }

    @Test
    public void testSaveUserWithSameEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        String email = TENANT_ADMIN_EMAIL;
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "User with email '" + email + "'  already present in database";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        String email = "tenant_thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "Invalid email address format '" + email + "'";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithEmptyEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "User email " + msgErrorShouldBeSpecified;
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("User email " + msgErrorShouldBeSpecified)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithoutTenant() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenant2@thingsboard.org");
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "Tenant administrator should be assigned to tenant";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));

    }

    @Test
    public void testDeleteUser() throws Exception {
        loginSysAdmin();

        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        String userIdStr = savedUser.getId().getId().toString();
        doGet("/api/user/" + userIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString( msgErrorNoFound("User",userIdStr))));
    }

    @Test
    public void testFindTenantAdmins() throws Exception {
        loginSysAdmin();

        //here created a new tenant despite already created on AbstractWebTest and then delete the tenant properly on the last line
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with many admins");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 64;
        List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(doPost("/api/user", user, User.class));
        }

        User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        assertThat(tenantAdmins).as("admins list size").hasSameSizeAs(loadedTenantAdmins);
        assertThat(tenantAdmins).as("admins list content").isEqualTo(loadedTenantAdmins);

        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());

        pageLink = new PageLink(33);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindTenantAdminsByEmail() throws Exception {

        loginSysAdmin();

        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        final int NUMBER_OF_USERS = 124;

        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
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
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
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
                    new TypeReference<>() {
                    }, pageLink);
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
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedTenantAdminsEmail1.size();
        for (User user : loadedTenantAdminsEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, ActionType.DELETED, cntEntity, NUMBER_OF_USERS, cntEntity, new String());

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedTenantAdminsEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerUsers() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

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
                    new TypeReference<>() {
                    }, pageLink);
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
    }

    @Test
    public void testFindCustomerUsersByEmail() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

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
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
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
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
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
                    new TypeReference<>() {
                    }, pageLink);
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
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteUserWithDeleteRelationsOk() throws Exception {
        UserId userId = createUser().getId();
        testEntityDaoWithRelationsOk(tenantId, userId, "/api/user/" + userId);
    }

    @Ignore
    @Test
    public void testDeleteUserExceptionWithRelationsTransactional() throws Exception {
        UserId userId = createUser().getId();
        testEntityDaoWithRelationsTransactionalException(userDao, tenantId, userId, "/api/user/" + userId);
    }

    @Test
    public void givenInvalidPageLink_thenReturnError() throws Exception {
        loginTenantAdmin();

        String invalidSortProperty = "abc(abc)";

        ResultActions result = doGet("/api/users?page={page}&pageSize={pageSize}&sortProperty={sortProperty}", 0, 100, invalidSortProperty)
                .andExpect(status().isBadRequest());
        assertThat(getErrorMessage(result)).containsIgnoringCase("invalid sort property");
    }

    @Test
    public void testSaveUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = mapper.readTree("{\"A\":5, \"B\":10, \"E\":18}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode retrievedSettings = doGet("/api/user/settings", JsonNode.class);
        Assert.assertEquals(retrievedSettings, userSettings);
   }

    @Test
    public void testShouldNotSaveJsonWithRestrictedSymbols() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = mapper.readTree("{\"A.B\":5, \"E\":18}");
        doPost("/api/user/settings", userSettings).andExpect(status().isBadRequest());

        userSettings = mapper.readTree("{\"A,B\":5, \"E\":18}");
        doPost("/api/user/settings", userSettings).andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = mapper.readTree("{\"A\":5, \"B\":{\"C\":true, \"D\":\"stringValue\"}}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode newSettings = mapper.readTree("{\"A\":10}");
        doPut("/api/user/settings", newSettings);
        JsonNode updatedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = mapper.readTree("{\"A\":10, \"B\":{\"C\":true, \"D\":\"stringValue\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        JsonNode patchedSettings = mapper.readTree("{\"A\":11, \"B\":{\"C\":false, \"D\":\"stringValue2\"}}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":11, \"B\":{\"C\":false, \"D\":\"stringValue2\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = mapper.readTree("{\"B.D\": \"stringValue3\"}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":11, \"B\":{\"C\":false, \"D\": \"stringValue3\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = mapper.readTree("{\"B.D\": {\"E\": 76, \"F\": 92}}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":11, \"B\":{\"C\":false, \"D\": {\"E\":76, \"F\": 92}}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = mapper.readTree("{\"B.D.E\": 100}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":11, \"B\":{\"C\":false, \"D\": {\"E\":100, \"F\": 92}}}");
        Assert.assertEquals(expectedSettings, updatedSettings);
    }

    @Test
    public void testShouldCreatePathIfNotExists() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = mapper.readTree("{\"A\":5}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode newSettings = mapper.readTree("{\"B\":{\"C\": 10}}");
        doPut("/api/user/settings", newSettings);
        JsonNode updatedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = mapper.readTree("{\"A\":5, \"B\":{\"C\": 10}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = mapper.readTree("{\"B.K\":true}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":5, \"B\":{\"C\": 10, \"K\": true}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = mapper.readTree("{\"B\":{}}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":5, \"B\":{}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = mapper.readTree("{\"F.G\":\"string\"}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":5, \"B\":{}, \"F\":{\"G\": \"string\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = mapper.readTree("{\"F\":{\"G\":\"string2\"}}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = mapper.readTree("{\"A\":5, \"B\":{}, \"F\":{\"G\": \"string2\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);
    }

    @Test
    public void testDeleteUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = mapper.readTree("{\"A\":10, \"B\":10, \"C\":{\"D\": 16}}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        doDelete("/api/user/settings/C.D,B");

        JsonNode retrievedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = mapper.readTree("{\"A\":10, \"C\":{}}");
        Assert.assertEquals(expectedSettings, retrievedSettings);
    }

    @Test
    public void shouldFindCustomerUsersByFirstName() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = savedCustomer.getId();

        Customer customer2 = new Customer();
        customer2.setTitle("My customer2");
        Customer savedCustomer2 = doPost("/api/customer", customer2, Customer.class);
        CustomerId customerId2 = savedCustomer2.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            user.setFirstName("Name" + i);
            user.setLastName("Lastname" + i);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId2);
            user.setFirstName("SecondCustomerName" + i);
            user.setLastName("SecondCustomerLastname" + i);
            user.setEmail("SecondCustomerUser" + i + "@thingsboard.org");
            doPost("/api/user", user, User.class);
        }

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setCustomerId(customerId);
        user.setEmail("testCustomerUser@thingsboard.org");
        createUserAndLogin(user, "testPassword2");

        // find user my name
        List<UserData> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(10, 0, "Name");
        PageData<UserData> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/users/info?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<UserData> customerUserDatas = customerUsers.stream().map(customerUser -> new UserData(customerUser.getId(),
                customerUser.getEmail(), customerUser.getFirstName(), customerUser.getLastName()))
                .sorted(userDataIdComparator).collect(Collectors.toList());
        loadedCustomerUsers.sort(userDataIdComparator);

        Assert.assertEquals(customerUserDatas, loadedCustomerUsers);

        // find user my full name
        loadedCustomerUsers.clear();
        pageLink = new PageLink(10, 0, "Name3");
        pageData = doGetTypedWithPageLink("/api/users/info?",
                new TypeReference<>() {
                }, pageLink);
        loadedCustomerUsers.addAll(pageData.getData());
        Assert.assertEquals(pageData.getData().size(), 1);
        Assert.assertEquals(pageData.getData().get(0).getEmail(), "testCustomer3@thingsboard.org");

        //clear users
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void shouldFindTenantUsersByLastName() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = savedCustomer.getId();

        Customer customer2 = new Customer();
        customer2.setTitle("My customer2");
        Customer savedCustomer2 = doPost("/api/customer", customer2, Customer.class);
        CustomerId customerId2 = savedCustomer2.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            user.setFirstName("Name" + i);
            user.setLastName("Lastname" + i);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId2);
            user.setFirstName("SecondCustomerName" + i);
            user.setLastName("SecondCustomerLastname" + i);
            user.setEmail("SecondCustomerUser" + i + "@thingsboard.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }

        // find user my name
        List<UserData> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(10, 0, "Name");
        PageData<UserData> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/users/info?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<UserData> customerUserDatas = customerUsers.stream().map(customerUser -> new UserData(customerUser.getId(),
                        customerUser.getEmail(), customerUser.getFirstName(), customerUser.getLastName()))
                .sorted(userDataIdComparator).collect(Collectors.toList());
        loadedCustomerUsers.sort(userDataIdComparator);

        Assert.assertEquals(customerUserDatas, loadedCustomerUsers);

        // find user my full name
        loadedCustomerUsers.clear();
        pageLink = new PageLink(10, 0, "SecondCustomerLastname3");
        pageData = doGetTypedWithPageLink("/api/users/info?",
                new TypeReference<>() {
                }, pageLink);
        loadedCustomerUsers.addAll(pageData.getData());
        Assert.assertEquals(pageData.getData().size(), 1);
        Assert.assertEquals(pageData.getData().get(0).getEmail(), "SecondCustomerUser3@thingsboard.org");

        //clear users
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    private User createUser() throws Exception {
        loginSysAdmin();
        String email = "tenant2@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");
        return doPost("/api/user", user, User.class);
    }
}
