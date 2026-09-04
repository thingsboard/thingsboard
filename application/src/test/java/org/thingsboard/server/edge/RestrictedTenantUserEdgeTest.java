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
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.mail.RecipientValidator;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.thingsboard.server.dao.user.UserServiceImpl.DEFAULT_TOKEN_LENGTH;

@DaoSqlTest
// The edge fixture's tenant runs on the "Default" tenant profile, so naming it here puts that edge under
// the restricted mail policy without standing up a second tenant with its own edge.
@TestPropertySource(properties = "security.restricted_tenant_profiles=Default")
public class RestrictedTenantUserEdgeTest extends AbstractEdgeTest {

    private static final String EDGE_USER_EMAIL = "restricted.edge.user@thingsboard.org";

    @Autowired
    private RecipientValidator recipientValidator;

    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    private UserService userService;

    @Before
    public void verifyFixtureTenantIsRestricted() {
        // Every test here is meaningless if the fixture's profile name ever leaves the configured list, and
        // the tests would go on passing while exercising the unrestricted paths. Fail loudly instead.
        assertThat(tenantProfileCache.isRestricted(tenantId)).isTrue();
    }

    @Test
    public void testEdgeCannotMarkUserActivated() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId userId = new UserId(UUID.randomUUID());
        UserCredentialsId credentialsId = new UserCredentialsId(UUID.randomUUID());

        sendUplinkAndWaitForResponse(buildUserUplinkMsg(userId, customerId, credentialsId, EDGE_USER_EMAIL));
        assertUserCredentialsFlags(userId, false, false);

        // The same uplink flips both flags to true on an unrestricted tenant - see
        // UserEdgeTest.testSendUserToCloudFromEdge, which asserts exactly that.
        sendUplinkAndWaitForResponse(buildEnabledCredentialsUplinkMsg(credentialsId, userId));
        assertUserCredentialsFlags(userId, false, false);

        // Which is what keeps the address out of the recipient allow-list: the allow-list keys on the
        // activation signal the uplink just failed to forge.
        TbEmail toEdgeUser = TbEmail.builder().to(EDGE_USER_EMAIL).subject("s").body("b").build();
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, toEdgeUser))
                .isInstanceOf(ThingsboardException.class);
    }

    @Test
    public void testEdgeCannotChangeUserEmail() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId userId = new UserId(UUID.randomUUID());

        sendUplinkAndWaitForResponse(buildUserUplinkMsg(userId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        User update = buildUser(customerId, "restricted.edge.renamed@thingsboard.org");
        update.setId(userId);
        update.setLastName("Renamed");
        sendUplinkAndWaitForResponse(UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, update))
                .build());

        // Acknowledged rather than failed: a failed uplink is resent by the edge until it succeeds.
        Assert.assertTrue(edgeImitator.getLatestResponseMsg().getSuccess());

        loginTenantAdmin();
        User cloudUser = doGet("/api/user/" + userId, User.class);
        Assert.assertEquals(EDGE_USER_EMAIL, cloudUser.getEmail());
        // The rest of the update still syncs; only the email is held back.
        Assert.assertEquals("Renamed", cloudUser.getLastName());
    }

    @Test
    public void testEdgeUserWithDuplicateEmailIsStillRenamed() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId firstUserId = new UserId(UUID.randomUUID());
        sendUplinkAndWaitForResponse(buildUserUplinkMsg(firstUserId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        // A second user carrying an email the cloud already holds: the processor renames it to an
        // unguessable address of its own, which is not a tenant-driven email change and must go through.
        UserId secondUserId = new UserId(UUID.randomUUID());
        sendUplinkAndWaitForResponse(buildUserUplinkMsg(secondUserId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        loginTenantAdmin();
        Assert.assertEquals(EDGE_USER_EMAIL, doGet("/api/user/" + firstUserId, User.class).getEmail());
        Assert.assertNotEquals(EDGE_USER_EMAIL, doGet("/api/user/" + secondUserId, User.class).getEmail());
    }

    @Test
    public void testCredentialsRequestDownlinkCarriesNoToken() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId userId = new UserId(UUID.randomUUID());
        sendUplinkAndWaitForResponse(buildUserUplinkMsg(userId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        String storedActivateToken = storedActivateToken(userId);

        // The cheapest route to a credentials downlink: the edge just asks for the user's credentials by id.
        edgeImitator.expectMessageAmount(1);
        sendUplinkAndWaitForResponse(UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserCredentialsRequestMsg(UserCredentialsRequestMsg.newBuilder()
                        .setUserIdMSB(userId.getId().getMostSignificantBits())
                        .setUserIdLSB(userId.getId().getLeastSignificantBits())
                        .build())
                .build());

        assertDownlinkCarriesNoToken(userId, storedActivateToken);
    }

    @Test
    public void testUserUpdateDownlinkCarriesNoToken() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId userId = new UserId(UUID.randomUUID());
        sendUplinkAndWaitForResponse(buildUserUplinkMsg(userId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        String storedActivateToken = storedActivateToken(userId);

        // The route the restricted email block itself opens on demand: refusing the edge's email change
        // writes a USER/UPDATED event addressed back to this same edge, and that downlink carries the
        // user's credentials alongside the user.
        User update = buildUser(customerId, "restricted.edge.downlink@thingsboard.org");
        update.setId(userId);
        edgeImitator.expectMessageAmount(2);
        sendUplinkAndWaitForResponse(UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, update))
                .build());

        assertDownlinkCarriesNoToken(userId, storedActivateToken);
    }

    @Test
    public void testEdgeCreatedUserIsMailedTheActivationLink() throws Exception {
        CustomerId customerId = createAndAssignCustomerToEdge().getId();
        UserId userId = new UserId(UUID.randomUUID());

        // clearInvocations (not reset) keeps the base class's activation stub armed.
        Mockito.clearInvocations(mailService);
        sendUplinkAndWaitForResponse(buildUserUplinkMsg(userId, customerId, new UserCredentialsId(UUID.randomUUID()), EDGE_USER_EMAIL));

        // With the token gone from the downlink, mailing the link is the only route left to activation, and
        // it reaches the address owner rather than the tenant that stood up the edge.
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mailService).sendActivationEmail(eq(tenantId), link.capture(), anyLong(), eq(EDGE_USER_EMAIL));
        assertThat(link.getValue()).endsWith(storedActivateToken(userId));
    }

    private String storedActivateToken(UserId userId) {
        UserCredentials stored = userService.findUserCredentialsByUserId(tenantId, userId);
        assertThat(stored).isNotNull();
        return stored.getActivateToken();
    }

    private void assertDownlinkCarriesNoToken(UserId userId, String storedActivateToken) {
        UserCredentialsUpdateMsg downlink = awaitCredentialsDownlink();
        UserCredentials sent = JacksonUtil.fromString(downlink.getEntity(), UserCredentials.class, true);
        assertThat(sent).isNotNull();
        // Positive control: this is the user under test, so the assertions below are about the right message.
        assertThat(sent.getUserId()).isEqualTo(userId);
        assertThat(sent.isEnabled()).isFalse();

        // The cloud does hold a live activation token for this user...
        assertThat(storedActivateToken).isNotBlank();
        // ...and no part of what reached the tenant-controlled edge carries it, or any reset token.
        assertThat(sent.getActivateToken()).isNull();
        assertThat(sent.getActivateTokenExpTime()).isNull();
        assertThat(sent.getResetToken()).isNull();
        assertThat(sent.getResetTokenExpTime()).isNull();
        assertThat(downlink.getEntity()).doesNotContain(storedActivateToken);
    }

    private UserCredentialsUpdateMsg awaitCredentialsDownlink() {
        AtomicReference<UserCredentialsUpdateMsg> received = new AtomicReference<>();
        // Polled rather than latched on an exact message count, so the assertion does not depend on how many
        // sub-messages the cloud happens to bundle into the downlink.
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<UserCredentialsUpdateMsg> downlink = edgeImitator.findMessageByType(UserCredentialsUpdateMsg.class);
            assertThat(downlink).as("UserCredentialsUpdateMsg downlink").isPresent();
            received.set(downlink.get());
        });
        return received.get();
    }

    private Customer createAndAssignCustomerToEdge() throws Exception {
        loginTenantAdmin();
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Restricted Edge Customer " + System.currentTimeMillis());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId() + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        return savedCustomer;
    }

    private void sendUplinkAndWaitForResponse(UplinkMsg uplinkMsg) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private User buildUser(CustomerId customerId, String email) {
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setEmail(email);
        user.setFirstName("Boris");
        user.setLastName("Johnson");
        return user;
    }

    private UplinkMsg buildUserUplinkMsg(UserId userId, CustomerId customerId, UserCredentialsId credentialsId, String email) {
        User user = buildUser(customerId, email);
        user.setId(userId);

        UserCredentials credentials = buildCredentials(credentialsId, userId, false);
        credentials.setActivateToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));

        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, user))
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(credentials))
                .build();
    }

    private UplinkMsg buildEnabledCredentialsUplinkMsg(UserCredentialsId credentialsId, UserId userId) {
        UserCredentials credentials = buildCredentials(credentialsId, userId, true);
        credentials.setPassword("password");
        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(credentials))
                .build();
    }

    private UserCredentials buildCredentials(UserCredentialsId credentialsId, UserId userId, boolean enabled) {
        UserCredentials credentials = new UserCredentials();
        credentials.setId(credentialsId);
        credentials.setUserId(userId);
        credentials.setEnabled(enabled);
        credentials.setAdditionalInfo(JacksonUtil.newObjectNode());
        return credentials;
    }

    private void assertUserCredentialsFlags(UserId userId, boolean enabled, boolean activated) throws Exception {
        loginTenantAdmin();
        User user = doGet("/api/user/" + userId, User.class);
        Assert.assertNotNull(user.getAdditionalInfo());
        Assert.assertEquals(enabled, user.getAdditionalInfo().get("userCredentialsEnabled").asBoolean());
        Assert.assertEquals(activated, user.getAdditionalInfo().get("userActivated").asBoolean());
    }

}
