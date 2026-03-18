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

            User createdUser = client.saveUser(user, "false");
            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals(email, createdUser.getEmail());
            assertEquals(Authority.TENANT_ADMIN, createdUser.getAuthority());

            createdUsers.add(createdUser);
        }

        // find all tenant admins, check count (20 created + 1 from setup)
        PageDataUser allUsers = client.getUsers(100, 0, null, null, null);
        assertNotNull(allUsers);
        assertNotNull(allUsers.getData());
        int initialSize = allUsers.getData().size();
        assertEquals("Expected 21 users (20 created + 2 from setup), but got " + initialSize, 22, initialSize);

        // find with search text, check count
        PageDataUser filteredUsers = client.getUsers(100, 0, TEST_PREFIX_2, null, null);
        assertEquals("Expected exactly 10 users matching prefix", 10, filteredUsers.getData().size());

        // find by id
        User searchUser = createdUsers.get(10);
        User fetchedUser = client.getUserById(searchUser.getId().getId().toString());
        assertEquals(searchUser.getEmail(), fetchedUser.getEmail());
        assertEquals(searchUser.getFirstName(), fetchedUser.getFirstName());

        // update user
        fetchedUser.setFirstName("UpdatedFirst");
        fetchedUser.setLastName("UpdatedLast");
        User updatedUser = client.saveUser(fetchedUser, "false");
        assertEquals("UpdatedFirst", updatedUser.getFirstName());
        assertEquals("UpdatedLast", updatedUser.getLastName());

        // activate user and get token
        activateUser(createdUsers.get(0).getId(), "password123", false);
        JwtPair userToken = client.getUserToken(createdUsers.get(0).getId().getId().toString());
        assertNotNull(userToken);
        assertNotNull(userToken.getToken());

        // disable user credentials
        client.setUserCredentialsEnabled(createdUsers.get(0).getId().getId().toString(), "false");

        // re-enable user credentials
        client.setUserCredentialsEnabled(createdUsers.get(0).getId().getId().toString(), "true");

        // create customer users and verify listing
        Customer customer2 = new Customer();
        customer2.setTitle("User test customer " + timestamp);
        customer2.setEmail("usertest_" + timestamp + "@test.com");
        Customer savedCustomer2 = client.saveCustomer(customer2, null, null, null);

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User customerUser = new User();
            customerUser.setEmail("custuser_" + timestamp + "_" + i + "@test.com");
            customerUser.setAuthority(Authority.CUSTOMER_USER);
            customerUser.setTenantId(savedClientTenant.getId());
            customerUser.setCustomerId(savedCustomer2.getId());
            customerUser.setFirstName("CustFirst" + i);
            customerUser.setLastName("CustLast" + i);

            User created = client.saveUser(customerUser, "false");
            assertNotNull(created);
            customerUsers.add(created);
        }

        // list customer users
        PageDataUser customerUserPage = client.getCustomerUsers(
                savedCustomer2.getId().getId().toString(), 100, 0, null, null, null);
        assertEquals("Expected 5 customer users", 5, customerUserPage.getData().size());

        // delete user
        UUID userToDeleteId = createdUsers.get(0).getId().getId();
        client.deleteUser(userToDeleteId.toString());

        // verify deletion
        PageDataUser usersAfterDelete = client.getUsers(100, 0, null, null, null);
        assertEquals(initialSize + 5 - 1, usersAfterDelete.getData().size());

        assertReturns404(() ->
                client.getUserById(userToDeleteId.toString())
        );
    }

}
