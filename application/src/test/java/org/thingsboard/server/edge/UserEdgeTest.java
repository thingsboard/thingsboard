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
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.user.UserServiceImpl.DEFAULT_TOKEN_LENGTH;

@DaoSqlTest
public class UserEdgeTest extends AbstractEdgeTest {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final String DEFAULT_FIRST_NAME = "Boris";
    private static final String UPDATED_LAST_NAME = "Borisov";

    @Test
    public void testCreateUpdateDeleteTenantUser() throws Exception {
        // create user
        edgeImitator.expectMessageAmount(3);
        User newTenantAdmin = buildUser(Authority.TENANT_ADMIN, null, "tenantAdmin@thingsboard.org", DEFAULT_FIRST_NAME, "Johnson");
        User savedTenantAdmin = createUser(newTenantAdmin, "tenant");
        Assert.assertTrue(edgeImitator.waitForMessages()); // wait 3 messages - x1 user update msg and x2 user credentials update msgs (create + authenticate user)
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserUpdateMsg.class).size());
        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size());

        UserUpdateMsg userUpdateMsg = getLatestUserUpdateMsg();
        User userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());

        // update user
        edgeImitator.expectMessageAmount(2);
        savedTenantAdmin.setLastName(UPDATED_LAST_NAME);
        savedTenantAdmin = doPost("/api/user", savedTenantAdmin, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        userUpdateMsg = getLatestUserUpdateMsg();
        User userFromMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_LAST_NAME, userFromMsg.getLastName());

        // update user credentials
        login(savedTenantAdmin.getEmail(), "tenant");

        edgeImitator.expectMessageAmount(1);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword("newTenant");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(savedTenantAdmin.getId(), userCredentialsMsg.getUserId());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsMsg.getPassword()));

        loginTenantAdmin();

        // delete user
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/user/" + savedTenantAdmin.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
    }

    @Test
    public void testCreateUpdateDeleteCustomerUser() throws Exception {
        // create customer
        Customer savedCustomer = createAndAssignCustomerToEdge("Edge Customer");

        // create user
        edgeImitator.expectMessageAmount(3);
        User customerUser = buildUser(Authority.CUSTOMER_USER, savedCustomer.getId(), "customerUser@thingsboard.org", "John", "Edwards");
        User savedCustomerUser = createUser(customerUser, "customer");
        Assert.assertTrue(edgeImitator.waitForMessages());  // wait 3 messages - x1 user update msg and x2 user credentials update msgs (create + authenticate user)
        Assert.assertEquals(1, edgeImitator.findAllMessagesByType(UserUpdateMsg.class).size());
        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size());
        Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        User userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getId(), userMsg.getId());
        Assert.assertEquals(savedCustomerUser.getCustomerId(), userMsg.getCustomerId());
        Assert.assertEquals(savedCustomerUser.getAuthority(), userMsg.getAuthority());
        Assert.assertEquals(savedCustomerUser.getEmail(), userMsg.getEmail());
        Assert.assertEquals(savedCustomerUser.getFirstName(), userMsg.getFirstName());
        Assert.assertEquals(savedCustomerUser.getLastName(), userMsg.getLastName());

        // update user
        edgeImitator.expectMessageAmount(2);
        savedCustomerUser.setLastName("Addams");
        savedCustomerUser = doPost("/api/user", savedCustomerUser, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        userUpdateMsg = userUpdateMsgOpt.get();
        userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getLastName(), userMsg.getLastName());

        // update user credentials
        login(savedCustomerUser.getEmail(), "customer");

        edgeImitator.expectMessageAmount(1);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("customer");
        changePasswordRequest.setNewPassword("newCustomer");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(savedCustomerUser.getId(), userCredentialsMsg.getUserId());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsMsg.getPassword()));

        loginTenantAdmin();

        // delete user
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/user/" + savedCustomerUser.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomerUser.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());

    }

    @Test
    public void testSendUserToCloudFromEdge() throws Exception {
        // create customer
        Customer savedCustomer = createAndAssignCustomerToEdge("Edge Customer");

        // create user
        User customerUser = buildUser(Authority.CUSTOMER_USER, savedCustomer.getId(), "customerUser@thingsboard.org", DEFAULT_FIRST_NAME, "Johnson");

        UUID uuid = UUID.randomUUID();
        customerUser.setId(new UserId(uuid));
        UUID userCredentialsUuid = UUID.randomUUID();
        UplinkMsg uplinkMsg = constructUserUplinkMsg(customerUser, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userCredentialsUuid);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());

        User userFromCloud = doGet("/api/user/" + uuid, User.class);
        Assert.assertNotNull(userFromCloud);
        Assert.assertEquals(customerUser.getEmail(), userFromCloud.getEmail());
        //check user with existing email
        User userWithExistingEmail = buildUser(Authority.CUSTOMER_USER, savedCustomer.getId(), "customerUser@thingsboard.org", DEFAULT_FIRST_NAME, "Johnson");

        UUID uuidForExistingEmail = UUID.randomUUID();
        userWithExistingEmail.setId(new UserId(uuidForExistingEmail));
        UUID userCredentialsUuidForExistingEmail = UUID.randomUUID();
        UplinkMsg uplinkMsgForExistingEmail = constructUserUplinkMsg(userWithExistingEmail, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userCredentialsUuidForExistingEmail);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgForExistingEmail);
        Assert.assertTrue(edgeImitator.waitForResponses());

        User userFromCloudWithExistingEmail = doGet("/api/user/" + uuidForExistingEmail, User.class);
        Assert.assertNotNull(userFromCloudWithExistingEmail);
        Assert.assertNotEquals(userWithExistingEmail.getEmail(), userFromCloudWithExistingEmail.getEmail());

        assertUserCredentialsFlags(userFromCloud, false, false);

        UplinkMsg enabledCredentialsUplinkMsg = constructUserCredentialsUplinkMsg(customerUser.getId(), "password", true, userCredentialsUuid);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(enabledCredentialsUplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());

        User cloudUserWithCredentials = doGet("/api/user/" + uuid, User.class);
        Assert.assertNotNull(cloudUserWithCredentials);

        assertUserCredentialsFlags(cloudUserWithCredentials, true, true);
    }

    @Test
    public void testSendUserCredentialsRequest() throws Exception {
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

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(tenantAdminUserId, userCredentialsMsg.getUserId());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }

    private User buildUser(Authority authority, CustomerId customerId, String email, String firstName, String lastName) {
        User customerUser = new User();
        customerUser.setAuthority(authority);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(customerId);
        customerUser.setEmail(email);
        customerUser.setFirstName(firstName);
        customerUser.setLastName(lastName);
        return customerUser;
    }

    private Customer createAndAssignCustomerToEdge(String title) throws Exception {
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle(title);
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId() + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        return savedCustomer;
    }

    private UplinkMsg constructUserUplinkMsg(User user, UpdateMsgType msgType, UUID userCredentialsUuid) {
        UserUpdateMsg userUpdateMsg = EdgeMsgConstructorUtils.constructUserUpdatedMsg(msgType, user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setId(new UserCredentialsId(userCredentialsUuid));
        userCredentials.setUserId(user.getId());
        userCredentials.setEnabled(false);
        userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
        UserCredentialsUpdateMsg userCredentialsMsg = EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials);

        return UplinkMsg.newBuilder()
                .addUserUpdateMsg(userUpdateMsg)
                .addUserCredentialsUpdateMsg(userCredentialsMsg)
                .build();
    }

    private UplinkMsg constructUserCredentialsUplinkMsg(UserId userId, String password, boolean enabled, UUID userCredentialsUuid) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setId(new UserCredentialsId(userCredentialsUuid));
        userCredentials.setUserId(userId);
        userCredentials.setEnabled(enabled);
        userCredentials.setPassword(password);
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

}
