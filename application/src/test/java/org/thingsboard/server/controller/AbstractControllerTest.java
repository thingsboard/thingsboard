// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractControllerTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@EnableWebSocket
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class AbstractControllerTest extends AbstractNotifyEntityTest {

    public static final String WS_URL = "ws://localhost:";

    @LocalServerPort
    protected int serverPort;

    protected volatile TbTestWebSocketClient wsClient; // lazy
    protected volatile TbTestWebSocketClient anotherWsClient; // lazy

    public TbTestWebSocketClient getWsClient() {
        if (wsClient == null) {
            synchronized (this) {
                try {
                    if (wsClient == null) {
                        wsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return wsClient;
    }

    public TbTestWebSocketClient getAnotherWsClient() {
        if (anotherWsClient == null) {
            synchronized (this) {
                try {
                    if (anotherWsClient == null) {
                        anotherWsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return anotherWsClient;
    }

    @Before
    public void beforeWsTest() {
        // placeholder
    }

    @After
    public void afterWsTest() {
        if (wsClient != null) {
            wsClient.close();
        }
        if (anotherWsClient != null) {
            anotherWsClient.close();
        }
    }

    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        return buildAndConnectWebSocketClient("/api/ws");
    }

    protected TbTestWebSocketClient buildAndConnectWebSocketClient(String path) throws URISyntaxException, InterruptedException {
        TbTestWebSocketClient wsClient = new TbTestWebSocketClient(new URI(WS_URL + serverPort + path));
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        if (!path.contains("token=")) {
            wsClient.authenticate(token);
        }
        return wsClient;
    }

    protected TbTestWebSocketClient buildAndConnectWebSocketClientWithApiKey(String apiKey) throws URISyntaxException, InterruptedException {
        TbTestWebSocketClient wsClient = new TbTestWebSocketClient(new URI(WS_URL + serverPort + "/api/ws"));
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        wsClient.authenticateWithApiKey(apiKey);
        return wsClient;
    }

    protected void resetSysAdminWhiteLabelingSettings() throws Exception {
        loginSysAdmin();

        doPost("/api/whiteLabel/loginWhiteLabelParams", new LoginWhiteLabelingParams(), LoginWhiteLabelingParams.class);
        doPost("/api/whiteLabel/whiteLabelParams", new WhiteLabelingParams(), WhiteLabelingParams.class);
    }

    protected EntityGroupInfo createSharedPublicEntityGroup(String name, EntityType entityType, EntityId ownerId) throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(name);
        entityGroup.setType(entityType);
        EntityGroupInfo groupInfo =
                doPostWithResponse("/api/entityGroup", entityGroup, EntityGroupInfo.class);
        var groupId = groupInfo.getId();

        shareGroup(groupId, ownerId);

        doPost("/api/entityGroup/" + groupId + "/makePublic")
                .andExpect(status().isOk());
        return doGet("/api/entityGroup/" + groupId, EntityGroupInfo.class);
    }

    protected void shareGroup(EntityGroupId entityGroupId, EntityId ownerId) throws Exception {
        ShareGroupRequest groupRequest = new ShareGroupRequest(
                ownerId,
                true,
                null,
                true,
                null
        );

        doPost("/api/entityGroup/" + entityGroupId + "/share", groupRequest)
                .andExpect(status().isOk());
    }

    protected void createPublicCustomerOnTenantLevel() throws Exception {
        createPublicGroupAndDeleteAfter(tenantId);
    }

    protected void createPublicCustomerOnCustomerLevel(Customer customer) throws Exception {
        User tmpUser = new User();
        tmpUser.setAuthority(Authority.CUSTOMER_USER);
        tmpUser.setTenantId(tenantId);
        tmpUser.setCustomerId(customer.getId());
        tmpUser.setEmail("tmpCustomerUser@thingsboard.org");
        User savedUser = createUser(tmpUser, "customer", findCustomerAdminsGroup(customer.getId()).getId());
        login("tmpCustomerUser@thingsboard.org", "customer");

        createPublicGroupAndDeleteAfter(customer.getId());

        loginTenantAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void createPublicGroupAndDeleteAfter(EntityId ownerId) throws Exception {
        EntityGroup tmpEntityGroup = new EntityGroup();
        tmpEntityGroup.setType(EntityType.DEVICE);
        tmpEntityGroup.setName(ownerId.getId().toString());
        tmpEntityGroup.setOwnerId(ownerId);
        tmpEntityGroup = doPost("/api/entityGroup", tmpEntityGroup, EntityGroup.class);

        doPost("/api/entityGroup/" + tmpEntityGroup.getUuidId() + "/makePublic").andExpect(status().isOk());

        doDelete("/api/entityGroup/" + tmpEntityGroup.getId().getId().toString()).andExpect(status().isOk());
    }

    protected Role createGenericRole(CustomerId customerId, String name, Map<Resource, List<Operation>> permissions) {
        return createRole(customerId, name, RoleType.GENERIC, permissions);
    }

    protected Role createGroupRole(CustomerId customerId, String name, List<Operation> permissions) {
        return createRole(customerId, name, RoleType.GROUP, permissions);
    }

    protected Role createRole(CustomerId customerId, String name, RoleType roleType, Object permissions) {
        Role role = new Role();
        role.setCustomerId(customerId);
        role.setName(name);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.valueToTree(permissions));
        return doPost("/api/role", role, Role.class);
    }

    protected GroupPermission createGroupPermission(EntityGroupId userGroupId, RoleId genericRoleId) {
        return createGroupPermission(userGroupId, genericRoleId, null, null);
    }

    protected GroupPermission createGroupPermission(EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return doPost("/api/groupPermission", groupPermission, GroupPermission.class);
    }

    /**
     * Creates a TENANT_ADMIN whose effective permissions are exactly the given map, then logs in as that user.
     * <p>
     * The caller must currently be logged in as a tenant admin of {@code tenantId} (so the necessary
     * POST /api/* calls are authorized). The new user is placed into a dedicated, freshly-created user group
     * bound (via {@link GroupPermission}) to a brand-new generic {@link Role} carrying the supplied permissions.
     * The user is NOT placed into the autogenerated "Tenant Administrators" group, so the supplied permissions
     * are the user's complete set.
     *
     * @return the newly-created restricted tenant admin {@link User}.
     */
    protected User loginAsRestrictedTenantAdmin(TenantId tenantId, Map<Resource, List<Operation>> permissions) throws Exception {
        return loginAsRestrictedTenantAdmin(tenantId, permissions, Map.of());
    }

    protected User loginAsRestrictedTenantAdmin(TenantId tenantId,
                                                Map<Resource, List<Operation>> permissions,
                                                Map<Resource, List<Operation>> excludedPermissions) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setName("Restricted Admins " + suffix);
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(tenantId);
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        Role role = new Role();
        role.setName("Restricted Role " + suffix);
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.valueToTree(permissions));
        if (excludedPermissions != null && !excludedPermissions.isEmpty()) {
            role.setExcludedPermissions(JacksonUtil.valueToTree(excludedPermissions));
        }
        role = doPost("/api/role", role, Role.class);

        createGroupPermission(userGroup.getId(), role.getId());

        String email = "restricted-admin-" + suffix + "@thingsboard.org";
        String password = "restricted-" + suffix;
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        User saved = createUser(user, password, userGroup.getId());

        login(email, password);
        return saved;
    }

    /**
     * Convenience overload: each readable {@link Resource} gets {@link Operation#READ}, each writable one
     * gets the full CRUD set ({@link Operation#READ}, {@link Operation#CREATE}, {@link Operation#WRITE},
     * {@link Operation#DELETE}). Resources appearing in both sets receive the union (effectively the writable set).
     */
    protected User loginAsRestrictedTenantAdmin(TenantId tenantId, Set<Resource> readableResources, Set<Resource> writableResources) throws Exception {
        Map<Resource, List<Operation>> permissions = new LinkedHashMap<>();
        for (Resource resource : readableResources) {
            permissions.put(resource, List.of(Operation.READ));
        }
        for (Resource resource : writableResources) {
            permissions.put(resource, List.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        }
        return loginAsRestrictedTenantAdmin(tenantId, permissions);
    }

}
