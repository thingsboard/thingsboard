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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.ActivateUserRequest;
import org.thingsboard.client.model.Authority;
import org.thingsboard.client.model.JwtPair;
import org.thingsboard.client.model.User;
import org.thingsboard.client.model.UserId;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.controller.AbstractControllerTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
public abstract class AbstractApiClientTest extends AbstractControllerTest {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String TEST_PREFIX = "ApiClientTestDevice_";
    protected static final String TEST_PREFIX_2 = "ApiClientTestDevice2_";
    protected static final String CUSTOMER_USERNAME = "javaClientCustomer@thingsboard.org";
    protected static final String TENANT_ADMIN_USERNAME = "javaClientTenant@thingsboard.org";
    protected static final String TEST_PASSWORD = "password123";

    protected ThingsboardClient client;

    // FQN for Tenant/Customer to avoid collision with AbstractWebTest fields
    protected org.thingsboard.client.model.Tenant savedClientTenant;
    protected User clientTenantAdmin;
    protected org.thingsboard.client.model.Customer savedClientCustomer;
    protected User savedClientCustomerUser;

    @Before
    public void setUpJavaClient() throws Exception {
        client = ThingsboardClient.builder()
                .url("http://localhost:" + wsPort)
                .build();
        client.login("sysadmin@thingsboard.org", "sysadmin");

        org.thingsboard.client.model.Tenant tenant = new org.thingsboard.client.model.Tenant();
        tenant.setTitle("Java client test tenant");
        savedClientTenant = client.saveTenant(tenant);

        clientTenantAdmin = new User();
        clientTenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        clientTenantAdmin.setTenantId(savedClientTenant.getId());
        clientTenantAdmin.setEmail(TENANT_ADMIN_USERNAME);
        clientTenantAdmin = client.saveUser(clientTenantAdmin, "false");
        activateUserAndAuthorize(clientTenantAdmin);

        org.thingsboard.client.model.Customer customer = new org.thingsboard.client.model.Customer();
        customer.setTitle("Java client test customer");
        customer.setTenantId(savedClientTenant.getId());
        savedClientCustomer = client.saveCustomer(customer, null, null, null);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedClientTenant.getId());
        customerUser.setCustomerId(savedClientCustomer.getId());
        customerUser.setEmail(CUSTOMER_USERNAME);
        savedClientCustomerUser = client.saveUser(customerUser, "false");
        activateUser(savedClientCustomerUser.getId(), "password123", false);
    }

    @After
    public void tearDownJavaClient() {
        client.login("sysadmin@thingsboard.org", "sysadmin");
        client.deleteTenant(savedClientTenant.getId().getId().toString());
    }

    protected String getBaseUrl() {
        return "http://localhost:" + wsPort;
    }

    protected void activateUserAndAuthorize(User user) throws ApiException {
        JwtPair jwtPair = activateUser(user.getId(), TEST_PASSWORD, false);
        client.setToken(jwtPair.getToken());
    }

    protected JwtPair activateUser(UserId userId, String password, boolean sendActivationMail) throws ApiException {
        ActivateUserRequest activateRequest = new ActivateUserRequest();
        activateRequest.setActivateToken(getActivateToken(userId));
        activateRequest.setPassword(password);
        return client.activateUser(activateRequest, sendActivationMail);
    }

    protected String getActivateToken(UserId userId) throws ApiException {
        String activateTokenRegex = "/api/noauth/activate?activateToken=";
        String activationLink = client.getActivationLink(userId.getId().toString());
        return activationLink.substring(activationLink.lastIndexOf(activateTokenRegex) + activateTokenRegex.length());
    }

    protected void assertReturns404(ThrowingRunnable operation) {
        try {
            operation.run();
            fail("Expected ApiException with 404 status code");
        } catch (ApiException exception) {
            assertEquals("Expected 404 status code but got " + exception.getCode(),
                    404, exception.getCode());
        } catch (Exception e) {
            fail("Expected ApiException but got " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

}
