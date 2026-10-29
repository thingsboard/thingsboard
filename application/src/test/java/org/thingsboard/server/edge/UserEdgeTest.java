// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
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
import static org.thingsboard.server.dao.user.UserServiceImpl.USER_PASSWORD_HISTORY;

@DaoSqlTest
public class UserEdgeTest extends AbstractEdgeTest {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

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
        // The initial USER ADDED edge event may bundle a UserCredentialsUpdateMsg when
        // user activation completes before the event is processed, in addition to the 2
        // messages from the CREDENTIALS_UPDATED events fired during activation. Accept 2 or 3.
        int credMsgCount = edgeImitator.findAllMessagesByType(UserCredentialsUpdateMsg.class).size();
        Assert.assertTrue("Expected 2 or 3 UserCredentialsUpdateMsg (ADDED/activation race), got " + credMsgCount,
                credMsgCount == 2 || credMsgCount == 3);

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

    @Test
    public void testSendUserWithTokenAuthorityFromEdgeToCloud() throws Exception {
        UserId userId = new UserId(UUID.randomUUID());
        User user = buildUser(Authority.REFRESH_TOKEN, null);
        user.setId(userId);

        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, user))
                .build());

        Assert.assertNull(userService.findUserById(tenantId, userId));
    }

    @Test
    public void testSendUserWithUnsupportedAuthorityFromEdgeToCloud() throws Exception {
        UserId userId = new UserId(UUID.randomUUID());
        User sysAdmin = buildUser(Authority.SYS_ADMIN, null);
        sysAdmin.setId(userId);
        sysAdmin.setTenantId(TenantId.SYS_TENANT_ID);
        UserCredentials userCredentials = buildCredentials(new UserCredentialsId(UUID.randomUUID()), userId, true);
        userCredentials.setPassword(passwordEncoder.encode("password"));

        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, sysAdmin))
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials))
                .build());

        Assert.assertNull(userService.findUserById(TenantId.SYS_TENANT_ID, userId));
    }

    @Test
    public void testSendUserWithDifferentTenantIdFromEdgeToCloud() throws Exception {
        UserId userId = new UserId(UUID.randomUUID());
        User tenantAdmin = buildUser(Authority.TENANT_ADMIN, null);
        tenantAdmin.setId(userId);
        tenantAdmin.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        UserCredentials userCredentials = buildCredentials(new UserCredentialsId(UUID.randomUUID()), userId, false);

        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, tenantAdmin))
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials))
                .build());

        User userFromCloud = doGet("/api/user/" + userId, User.class);
        Assert.assertEquals(tenantId, userFromCloud.getTenantId());
    }

    @Test
    public void testSendUserMsgsForUserOfAnotherTenantFromEdgeToCloud() throws Exception {
        User sysAdmin = userService.findUserByEmail(TenantId.SYS_TENANT_ID, SYS_ADMIN_EMAIL);
        UserCredentials sysAdminCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, sysAdmin.getId());

        // update, credentials update and delete for a user outside of edge's tenant
        User tenantAdmin = buildUser(Authority.TENANT_ADMIN, null);
        tenantAdmin.setId(sysAdmin.getId());
        UserCredentials userCredentials = buildCredentials(new UserCredentialsId(UUID.randomUUID()), sysAdmin.getId(), true);
        userCredentials.setPassword(passwordEncoder.encode("password"));
        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantAdmin))
                .build());
        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials))
                .build());
        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserDeleteMsg(sysAdmin.getId()))
                .build());

        User sysAdminAfter = userService.findUserById(TenantId.SYS_TENANT_ID, sysAdmin.getId());
        Assert.assertNotNull(sysAdminAfter);
        Assert.assertEquals(Authority.SYS_ADMIN, sysAdminAfter.getAuthority());
        Assert.assertEquals(TenantId.SYS_TENANT_ID, sysAdminAfter.getTenantId());
        UserCredentials sysAdminCredentialsAfter = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, sysAdmin.getId());
        Assert.assertEquals(sysAdminCredentials.getId(), sysAdminCredentialsAfter.getId());
        Assert.assertEquals(sysAdminCredentials.getPassword(), sysAdminCredentialsAfter.getPassword());
    }

    private void sendUplinkMsg(UplinkMsg uplinkMsg) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private UplinkMsg buildUserUplinkMsg(UserId userId, CustomerId customerId, UserCredentialsId userCredentialsUuid) {
        User customerUser = buildUser(Authority.CUSTOMER_USER, customerId);
        customerUser.setId(userId);
        UserCredentials userCredentials = buildCredentials(userCredentialsUuid, userId, false);
        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));

        UserUpdateMsg userUpdateMsg = EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUser);
        // built directly, because constructUserCredentialsUpdatedMsg drops activation tokens
        UserCredentialsUpdateMsg userCredentialsMsg = UserCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(userCredentials)).build();

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

    @Test
    public void testSendUserCredentialsRequestForUserOfDifferentTenantToCloud() throws Exception {
        loginDifferentTenant();
        UUID userUuid = savedDifferentTenantUser.getUuidId();
        loginTenantAdmin();
        sendUplinkMsgAndVerifyNoDownlink(uplinkMsgBuilder -> uplinkMsgBuilder.addUserCredentialsRequestMsg(UserCredentialsRequestMsg.newBuilder()
                .setUserIdMSB(userUuid.getMostSignificantBits())
                .setUserIdLSB(userUuid.getLeastSignificantBits()).build()));
    }

    @Test
    public void testUserActivationInDifferentTenant() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        verifyNoEdgeEvents(savedDifferentTenantUser.getId());
    }

    @Test
    public void testSysAdminCredentialsUpdate() throws Exception {
        loginSysAdmin();
        UserId sysAdminId = doGet("/api/auth/user", User.class).getId();
        resetTokens();
        doPost("/api/noauth/resetPasswordByEmail", JacksonUtil.newObjectNode().put("email", SYS_ADMIN_EMAIL))
                .andExpect(status().isOk());
        loginTenantAdmin();
        verifyNoEdgeEvents(sysAdminId);
    }

    @Test
    public void testTenantUserCredentialsUpdate() throws Exception {
        resetTokens();
        edgeImitator.expectMessageAmount(1);
        doPost("/api/noauth/resetPasswordByEmail", JacksonUtil.newObjectNode().put("email", TENANT_ADMIN_EMAIL))
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentials userCredentials = JacksonUtil.fromString(((UserCredentialsUpdateMsg) latestMessage).getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentials);
        Assert.assertEquals(tenantAdminUserId, userCredentials.getUserId());
        loginTenantAdmin();
    }

    private void verifyNoEdgeEvents(UserId userId) {
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), 0L, Long.MAX_VALUE, new TimePageLink(1000));
                    Assert.assertTrue(edgeEvents.getData().stream().noneMatch(edgeEvent -> userId.getId().equals(edgeEvent.getEntityId())));
                });
    }

    @Test
    public void testSendUserCredentialsWithIdOfDifferentTenantUserToCloud() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        UserCredentials differentTenantCredentials = userService.findUserCredentialsByUserId(differentTenantId, savedDifferentTenantUser.getId());
        UserCredentials userCredentials = buildCredentials(differentTenantCredentials.getId(), tenantAdminUserId, true);
        userCredentials.setPassword(passwordEncoder.encode("password"));

        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials))
                .build());

        UserCredentials differentTenantCredentialsAfter = userService.findUserCredentialsByUserId(differentTenantId, savedDifferentTenantUser.getId());
        Assert.assertNotNull(differentTenantCredentialsAfter);
        Assert.assertEquals(differentTenantCredentials.getPassword(), differentTenantCredentialsAfter.getPassword());
    }

    @Test
    public void testSendUserCredentialsWithTokensAndLoginStateFromEdgeToCloud() throws Exception {
        User user = buildUser(Authority.TENANT_ADMIN, null);
        user.setEmail("edgeCredentialsUser@thingsboard.org");
        User savedUser = doPost("/api/user?sendActivationMail=false", user, User.class);
        UserCredentials cloudCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        cloudCredentials.setResetToken("cloudResetToken");
        cloudCredentials.setResetTokenExpTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        cloudCredentials.setFailedLoginAttempts(3);
        userService.saveUserCredentials(tenantId, cloudCredentials, false);

        UserCredentials edgeCredentials = buildCredentials(cloudCredentials.getId(), savedUser.getId(), true);
        String edgePassword = passwordEncoder.encode("edgePassword");
        edgeCredentials.setPassword(edgePassword);
        edgeCredentials.setActivateToken("edgeActivateToken");
        edgeCredentials.setResetToken("edgeResetToken");
        edgeCredentials.setResetTokenExpTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        edgeCredentials.setFailedLoginAttempts(0);
        sendUplinkMsg(UplinkMsg.newBuilder()
                .addUserCredentialsUpdateMsg(UserCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(edgeCredentials)).build())
                .build());

        UserCredentials cloudCredentialsAfter = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        Assert.assertEquals(edgePassword, cloudCredentialsAfter.getPassword());
        Assert.assertTrue(cloudCredentialsAfter.isEnabled());
        Assert.assertNull(cloudCredentialsAfter.getActivateToken());
        Assert.assertEquals("cloudResetToken", cloudCredentialsAfter.getResetToken());
        Assert.assertEquals(Integer.valueOf(3), cloudCredentialsAfter.getFailedLoginAttempts());
        Assert.assertTrue(cloudCredentialsAfter.getAdditionalInfo().get(USER_PASSWORD_HISTORY).toString().contains(edgePassword));
    }

}
