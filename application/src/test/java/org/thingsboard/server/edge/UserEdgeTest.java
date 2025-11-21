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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.user.UserServiceImpl.DEFAULT_TOKEN_LENGTH;

@DaoSqlTest
public class UserEdgeTest extends AbstractEdgeTest {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String DEFAULT_FIRST_NAME = "Boris";
    private static final String DEFAULT_LAST_NAME = "Johnson";
    private static final String UPDATED_LAST_NAME = "Borisov";
    private static final String DEFAULT_TENANT_ADMIN_EMAIL = "tenantAdmin@thingsboard.org";
    private static final String DEFAULT_CUSTOMER_USER_EMAIL = "customerUser@thingsboard.org";

    @Test
    public void testCreateUpdateDeleteTenantUser() throws Exception {
        // create user
        User newTenantAdmin = buildUser(Authority.TENANT_ADMIN, null);
        User savedTenantAdmin = createAndVerifyUserOnEdge(newTenantAdmin);

        // update user
        updateAndVerifyUserLastName(savedTenantAdmin);

        // update user credentials
        login(savedTenantAdmin.getEmail(), "tenant");
        updateAndVerifyUserCredentials(savedTenantAdmin);
        loginTenantAdmin();

        // delete user
        deleteAndVerifyUser(savedTenantAdmin);
    }

    @Test
    public void testCreateUpdateDeleteCustomerUser() throws Exception {
        // create customer
        Customer savedCustomer = createAndAssignCustomerToEdge();

        // create user
        User customerUser = buildUser(Authority.CUSTOMER_USER, savedCustomer.getId());
        User savedCustomerUser = createAndVerifyUserOnEdge(customerUser);

        // update user
        updateAndVerifyUserLastName(savedCustomerUser);

        // update user credentials
        login(savedCustomerUser.getEmail(), "customer");
        updateAndVerifyUserCredentials(savedCustomerUser);
        loginTenantAdmin();

        // delete user
        deleteAndVerifyUser(savedCustomerUser);
    }

    @Test
    public void testSendUserToCloudFromEdge() throws Exception {
        // create customer
        Customer savedCustomer = createAndAssignCustomerToEdge();

        // create uplinkMsg with user and userCredentials
        UserId userId = new UserId(UUID.randomUUID());
        UserCredentialsId userCredentialsId = new UserCredentialsId(UUID.randomUUID());
        UplinkMsg uplinkMsg = buildUserUplinkMsg(userId, savedCustomer.getId(), userCredentialsId);

        User userFromCloud = verifyMsgOnCloud(uplinkMsg, userId, false);
        assertUserCredentialsFlags(userFromCloud, false, false);

        // create uplinkMsg with enabled userCredentials
        UplinkMsg uplinkMsgWithEnabledCredentials = constructUserCredentialsUplinkMsg(userCredentialsId, userId);

        User cloudUserWithCredentials = verifyMsgOnCloud(uplinkMsgWithEnabledCredentials, userId, false);
        assertUserCredentialsFlags(cloudUserWithCredentials, true, true);

        // create uplinkMsg with user the same email
        UserId secondUserId = new UserId(UUID.randomUUID());
        UserCredentialsId secondCredentialsId = new UserCredentialsId(UUID.randomUUID());
        UplinkMsg uplinkMsgForUserExistingEmail = buildUserUplinkMsg(secondUserId, savedCustomer.getId(), secondCredentialsId);

        verifyMsgOnCloud(uplinkMsgForUserExistingEmail, secondUserId, true);
    }

    @Test
    public void testSendUserCredentialsRequestToCloud() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdminUserId.getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdminUserId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        UserCredentialsUpdateMsg userCredentialsUpdateMsg = getLatestUserCredentialsUpdateMsg();
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(tenantAdminUserId, userCredentialsMsg.getUserId());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }

    @Test
    public void testSendUserDeleteFromEdgeToCloud() throws Exception {
        // create customer
        Customer savedCustomer = createAndAssignCustomerToEdge();

        // create user
        User customerUser = buildUser(Authority.CUSTOMER_USER, savedCustomer.getId());
        User savedCustomerUser = createAndVerifyUserOnEdge(customerUser);

        // simulate user removal event from edge to cloud
        UserUpdateMsg.Builder userUpdateMsg = UserUpdateMsg.newBuilder().setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(savedCustomerUser.getUuidId().getMostSignificantBits())
                .setIdLSB(savedCustomerUser.getUuidId().getLeastSignificantBits());

        UplinkMsg uplink = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserUpdateMsg(userUpdateMsg).build();

        testAutoGeneratedCodeByProtobuf(userUpdateMsg);

        // expect edge message sent & cloud message response
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplink);
        Assert.assertTrue(edgeImitator.waitForResponses());

        loginTenantAdmin();
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        doGet("/api/user/" + savedCustomerUser.getId(), User.class, status().isNotFound());
                        return true;
                    } catch (Throwable ex) {
                        return false;
                    }
                });
    }

    private Customer createAndAssignCustomerToEdge() throws Exception {
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId() + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        return savedCustomer;
    }

    private User buildUser(Authority authority, CustomerId customerId) {
        User user = new User();

        user.setAuthority(authority);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setEmail(authority == Authority.TENANT_ADMIN ? DEFAULT_TENANT_ADMIN_EMAIL : DEFAULT_CUSTOMER_USER_EMAIL);
        user.setFirstName(DEFAULT_FIRST_NAME);
        user.setLastName(DEFAULT_LAST_NAME);

        return user;
    }

    private User createAndVerifyUserOnEdge(User user) throws Exception {
        String password = user.getAuthority() == Authority.TENANT_ADMIN ? "tenant" : "customer";

        // wait 3 messages - x1 user update msg and x2 user credentials update msgs (create + authenticate user)
        edgeImitator.expectMessageAmount(3);
        User savedUser = createUser(user, password);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserUpdateMsg.class).size());
        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size());

        UserUpdateMsg userUpdateMsg = getLatestUserUpdateMsg();
        User userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedUser.getId(), userMsg.getId());
        Assert.assertEquals(savedUser.getCustomerId(), userMsg.getCustomerId());
        Assert.assertEquals(savedUser.getAuthority(), userMsg.getAuthority());
        Assert.assertEquals(savedUser.getEmail(), userMsg.getEmail());
        Assert.assertEquals(savedUser.getFirstName(), userMsg.getFirstName());
        Assert.assertEquals(savedUser.getLastName(), userMsg.getLastName());

        return savedUser;
    }

    private void updateAndVerifyUserLastName(User user) throws Exception {
        user.setLastName(UPDATED_LAST_NAME);

        edgeImitator.expectMessageAmount(2);
        doPost("/api/user", user, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UserUpdateMsg userUpdateMsg = getLatestUserUpdateMsg();
        User userFromMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_LAST_NAME, userFromMsg.getLastName());
    }

    private void updateAndVerifyUserCredentials(User user) throws Exception {
        String password = user.getAuthority() == Authority.TENANT_ADMIN ? "tenant" : "customer";
        String newPassword = "new" + password;

        edgeImitator.expectMessageAmount(1);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword(password);
        changePasswordRequest.setNewPassword(newPassword);
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UserCredentialsUpdateMsg msg = getLatestUserCredentialsUpdateMsg();
        UserCredentials creds = JacksonUtil.fromString(msg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(creds);
        Assert.assertEquals(user.getId(), creds.getUserId());
        Assert.assertTrue(passwordEncoder.matches(newPassword, creds.getPassword()));
    }

    private void deleteAndVerifyUser(User savedTenantAdmin) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/user/" + savedTenantAdmin.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        UserUpdateMsg userUpdateMsg = getLatestUserUpdateMsg();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
    }

    private UplinkMsg buildUserUplinkMsg(UserId userId, CustomerId customerId, UserCredentialsId userCredentialsUuid) {
        User customerUser = buildUser(Authority.CUSTOMER_USER, customerId);
        customerUser.setId(userId);
        UserCredentials userCredentials = buildCredentials(userCredentialsUuid, userId, false);
        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));

        UserUpdateMsg userUpdateMsg = EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUser);
        UserCredentialsUpdateMsg userCredentialsMsg = EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials);

        return UplinkMsg.newBuilder()
                .addUserUpdateMsg(userUpdateMsg)
                .addUserCredentialsUpdateMsg(userCredentialsMsg)
                .build();
    }

    private UserCredentials buildCredentials(UserCredentialsId userCredentialsUuid, UserId userId, boolean enabled) {
        UserCredentials userCredentials = new UserCredentials();

        userCredentials.setId(userCredentialsUuid);
        userCredentials.setUserId(userId);
        userCredentials.setEnabled(enabled);
        userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());

        return userCredentials;
    }

    private User verifyMsgOnCloud(UplinkMsg uplinkMsg, UserId userId, boolean emailExist) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());

        User userFromCloud = doGet("/api/user/" + userId, User.class);
        Assert.assertNotNull(userFromCloud);

        if (emailExist) {
            Assert.assertNotEquals(DEFAULT_CUSTOMER_USER_EMAIL, userFromCloud.getEmail());
        } else {
            Assert.assertEquals(DEFAULT_CUSTOMER_USER_EMAIL, userFromCloud.getEmail());
        }
        return userFromCloud;
    }

    private UplinkMsg constructUserCredentialsUplinkMsg(UserCredentialsId userCredentialsUuid, UserId userId) {
        UserCredentials userCredentials = buildCredentials(userCredentialsUuid, userId, true);
        userCredentials.setPassword("password");
        UserCredentialsUpdateMsg credsMsg = EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials);

        return UplinkMsg.newBuilder()
                .addUserCredentialsUpdateMsg(credsMsg)
                .build();
    }

    private void assertUserCredentialsFlags(User user, boolean enabled, boolean activated) {
        JsonNode info = user.getAdditionalInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(enabled, info.get("userCredentialsEnabled").asBoolean());
        Assert.assertEquals(activated, info.get("userActivated").asBoolean());
    }

    private UserUpdateMsg getLatestUserUpdateMsg() {
        Optional<UserUpdateMsg> opt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(opt.isPresent());
        return opt.get();
    }

    private UserCredentialsUpdateMsg getLatestUserCredentialsUpdateMsg() {
        Optional<UserCredentialsUpdateMsg> opt = edgeImitator.findMessageByType(UserCredentialsUpdateMsg.class);
        Assert.assertTrue(opt.isPresent());
        return opt.get();
    }

}
