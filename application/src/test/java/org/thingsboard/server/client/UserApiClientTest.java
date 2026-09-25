// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteUserArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerUsersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserTokenArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUsersArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveUserArgs;
import org.thingsboard.client.api.ThingsboardApi.SetUserCredentialsEnabledArgs;
import org.thingsboard.client.model.Authority;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.JwtPair;
import org.thingsboard.client.model.PageDataUser;
import org.thingsboard.client.model.User;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class UserApiClientTest extends AbstractApiClientTest {

    @Test
    public void testUserLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<User> createdUsers = new ArrayList<>();

        // create 20 tenant admin users
        for (int i = 0; i < 20; i++) {
            User user = new User();
            String email = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i + "@test.com";
            user.setEmail(email);
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(savedClientTenant.getId());
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            User createdUser = client.saveUser(SaveUserArgs.builder()
                    .user(user)
                    .sendActivationMail("false")
                    .build());
            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals(email, createdUser.getEmail());
            assertEquals(Authority.TENANT_ADMIN, createdUser.getAuthority());

            createdUsers.add(createdUser);
        }

        // find all tenant admins, check count (20 created + 1 from setup)
        PageDataUser allUsers = client.getUsers(GetUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allUsers);
        assertNotNull(allUsers.getData());
        int initialSize = allUsers.getData().size();
        assertEquals("Expected 21 users (20 created + 2 from setup), but got " + initialSize, 22, initialSize);

        // find with search text, check count
        PageDataUser filteredUsers = client.getUsers(GetUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 users matching prefix", 10, filteredUsers.getData().size());

        // find by id
        User searchUser = createdUsers.get(10);
        User fetchedUser = client.getUserById(GetUserByIdArgs.builder()
                .userId(searchUser.getId().getId().toString())
                .build());
        assertEquals(searchUser.getEmail(), fetchedUser.getEmail());
        assertEquals(searchUser.getFirstName(), fetchedUser.getFirstName());

        // update user
        fetchedUser.setFirstName("UpdatedFirst");
        fetchedUser.setLastName("UpdatedLast");
        User updatedUser = client.saveUser(SaveUserArgs.builder()
                .user(fetchedUser)
                .sendActivationMail("false")
                .build());
        assertEquals("UpdatedFirst", updatedUser.getFirstName());
        assertEquals("UpdatedLast", updatedUser.getLastName());

        // activate user and get token
        activateUser(createdUsers.get(0).getId(), "password123", false);
        JwtPair userToken = client.getUserToken(GetUserTokenArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .build());
        assertNotNull(userToken);
        assertNotNull(userToken.getToken());

        // disable user credentials
        client.setUserCredentialsEnabled(SetUserCredentialsEnabledArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .userCredentialsEnabled("false")
                .build());

        // re-enable user credentials
        client.setUserCredentialsEnabled(SetUserCredentialsEnabledArgs.builder()
                .userId(createdUsers.get(0).getId().getId().toString())
                .userCredentialsEnabled("true")
                .build());

        // create customer users and verify listing
        Customer customer2 = new Customer();
        customer2.setTitle("User test customer " + timestamp);
        customer2.setEmail("usertest_" + timestamp + "@test.com");
        Customer savedCustomer2 = client.saveCustomer(SaveCustomerArgs.builder()
                .customer(customer2)
                .build());

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User customerUser = new User();
            customerUser.setEmail("custuser_" + timestamp + "_" + i + "@test.com");
            customerUser.setAuthority(Authority.CUSTOMER_USER);
            customerUser.setTenantId(savedClientTenant.getId());
            customerUser.setCustomerId(savedCustomer2.getId());
            customerUser.setFirstName("CustFirst" + i);
            customerUser.setLastName("CustLast" + i);

            User created = client.saveUser(SaveUserArgs.builder()
                    .user(customerUser)
                    .sendActivationMail("false")
                    .build());
            assertNotNull(created);
            customerUsers.add(created);
        }

        // list customer users
        PageDataUser customerUserPage = client.getCustomerUsers(GetCustomerUsersArgs.builder()
                .customerId(savedCustomer2.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertEquals("Expected 5 customer users", 5, customerUserPage.getData().size());

        // delete user
        UUID userToDeleteId = createdUsers.get(0).getId().getId();
        client.deleteUser(DeleteUserArgs.builder()
                .userId(userToDeleteId.toString())
                .build());

        // verify deletion
        PageDataUser usersAfterDelete = client.getUsers(GetUsersArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize + 5 - 1, usersAfterDelete.getData().size());

        assertReturns404(() ->
                client.getUserById(GetUserByIdArgs.builder()
                        .userId(userToDeleteId.toString())
                        .build())
        );
    }

}
