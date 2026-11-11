// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class ApiKeyServiceTest extends AbstractServiceTest {

    private static final String TEST_API_KEY_DESCRIPTION = "Test API Key Description";

    @Autowired
    ApiKeyService apiKeyService;

    @Autowired
    UserService userService;

    private UserId userId;

    @Before
    public void before() {
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant@thingsboard.org");
        User user = userService.saveUser(TenantId.SYS_TENANT_ID, tenantAdmin);
        userId = user.getId();
    }

    @After
    public void after() {
        apiKeyService.deleteByTenantId(tenantId);
        User user = userService.findUserById(tenantId, userId);
        userService.deleteUser(tenantId, user);
    }

    @Test
    public void testSaveApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertNotNull(savedApiKey.getId());
        Assert.assertEquals(tenantId, savedApiKey.getTenantId());
        Assert.assertEquals(TEST_API_KEY_DESCRIPTION, savedApiKey.getDescription());
        Assert.assertTrue(savedApiKey.isEnabled());
        Assert.assertNotNull(savedApiKey.getValue());
    }

    @Test
    public void testSaveInternalApiKey() {
        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.WRITE));
        permissions.put(Resource.DASHBOARD, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.SYS_ADMIN, permissions);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        ApiKeyInfo apiKeyInfo = createInternalApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        apiKeyInfo.setPermissions(authorityPermissionsInfo);

        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertNotNull(savedApiKey.getId());
        Assert.assertEquals(TenantId.SYS_TENANT_ID, savedApiKey.getTenantId());
        Assert.assertEquals(TEST_API_KEY_DESCRIPTION, savedApiKey.getDescription());
        Assert.assertTrue(savedApiKey.isEnabled());
        Assert.assertTrue(savedApiKey.isInternal());
        Assert.assertEquals(0, savedApiKey.getExpirationTime());
        Assert.assertNotNull(savedApiKey.getValue());
        Assert.assertNotNull(savedApiKey.getPermissions());

        var sysAdminPermissions = savedApiKey.getPermissions().getPermissionsForAuthority(Authority.SYS_ADMIN);
        Assert.assertNotNull(sysAdminPermissions);
        Assert.assertEquals(2, sysAdminPermissions.size());
        Assert.assertTrue(sysAdminPermissions.containsKey(Resource.DEVICE));
        Assert.assertTrue(sysAdminPermissions.containsKey(Resource.DASHBOARD));
        Assert.assertEquals(2, sysAdminPermissions.get(Resource.DEVICE).size());
        Assert.assertTrue(sysAdminPermissions.get(Resource.DEVICE).contains(Operation.READ));
        Assert.assertTrue(sysAdminPermissions.get(Resource.DEVICE).contains(Operation.WRITE));
    }

    @Test
    public void testSaveApiKeyWithMultipleAuthorities() {
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();

        // SYS_ADMIN permissions
        Map<Resource, Set<Operation>> sysAdminPerms = new HashMap<>();
        sysAdminPerms.put(Resource.TENANT, Set.of(Operation.READ, Operation.WRITE));
        operationsByResource.put(Authority.SYS_ADMIN, sysAdminPerms);

        // TENANT_ADMIN permissions
        Map<Resource, Set<Operation>> tenantAdminPerms = new HashMap<>();
        tenantAdminPerms.put(Resource.DEVICE, Set.of(Operation.READ));
        tenantAdminPerms.put(Resource.ASSET, Set.of(Operation.READ, Operation.WRITE));
        operationsByResource.put(Authority.TENANT_ADMIN, tenantAdminPerms);

        // CUSTOMER_USER permissions
        Map<Resource, Set<Operation>> customerUserPerms = new HashMap<>();
        customerUserPerms.put(Resource.DASHBOARD, Set.of(Operation.READ));
        operationsByResource.put(Authority.CUSTOMER_USER, customerUserPerms);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        ApiKeyInfo apiKeyInfo = createInternalApiKeyInfo("Multi-authority internal key");
        apiKeyInfo.setPermissions(authorityPermissionsInfo);

        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertTrue(savedApiKey.isInternal());
        Assert.assertNotNull(savedApiKey.getPermissions());

        Assert.assertNotNull(savedApiKey.getPermissions().getPermissionsForAuthority(Authority.SYS_ADMIN));
        Assert.assertNotNull(savedApiKey.getPermissions().getPermissionsForAuthority(Authority.TENANT_ADMIN));
        Assert.assertNotNull(savedApiKey.getPermissions().getPermissionsForAuthority(Authority.CUSTOMER_USER));
    }

    @Test
    public void testSaveApiKeyWithTooLongDescription() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(StringUtils.randomAlphabetic(300));

        assertThatThrownBy(() -> apiKeyService.saveApiKey(tenantId, apiKeyInfo))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("description length must be equal or less than 255");
    }

    @Test
    public void testSaveRegularApiKeyCantSetInternal() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        apiKeyInfo.setInternal(true);

        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertFalse(savedApiKey.isInternal());
        Assert.assertNull(savedApiKey.getPermissions());
    }

    @Test
    public void testUpdateRegularApiKeyCantBecomeInternal() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo("Regular key");
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        Assert.assertFalse(savedApiKey.isInternal());

        savedApiKey.setInternal(true);

        savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertFalse(savedApiKey.isInternal());
    }

    @Test
    public void testUpdateDescriptionApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        String newDescription = "Updated API Key Description";
        savedApiKey.setDescription(newDescription);
        ApiKey updatedApiKey = apiKeyService.saveApiKey(tenantId, savedApiKey);

        Assert.assertNotNull(updatedApiKey);
        Assert.assertEquals(savedApiKey.getId(), updatedApiKey.getId());
        Assert.assertEquals(newDescription, updatedApiKey.getDescription());
        Assert.assertEquals(savedApiKey.getValue(), updatedApiKey.getValue());
    }

    @Test
    public void testUpdateDescriptionInternalApiKey_shouldFail() {
        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.SYS_ADMIN, permissions);

        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        ApiKeyInfo apiKeyInfo = createInternalApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        apiKeyInfo.setPermissions(authorityPermissionsInfo);

        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        Assert.assertTrue(savedApiKey.isInternal());

        String newDescription = "Updated internal key description";
        savedApiKey.setDescription(newDescription);

        assertThatThrownBy(() -> apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, savedApiKey))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Cannot update internal API key description!");
    }

    @Test
    public void testDisableApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        savedApiKey.setEnabled(false);
        ApiKey disabledApiKey = apiKeyService.saveApiKey(tenantId, savedApiKey);

        Assert.assertNotNull(disabledApiKey);
        Assert.assertEquals(savedApiKey.getId(), disabledApiKey.getId());
        Assert.assertFalse(disabledApiKey.isEnabled());
    }

    @Test
    public void testFindApiKeyById() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        ApiKey foundApiKey = apiKeyService.findApiKeyById(tenantId, savedApiKey.getId());

        Assert.assertNotNull(foundApiKey);
        Assert.assertEquals(savedApiKey.getId(), foundApiKey.getId());
        Assert.assertEquals(savedApiKey.getDescription(), foundApiKey.getDescription());
        Assert.assertEquals(savedApiKey.isEnabled(), foundApiKey.isEnabled());
        Assert.assertEquals(savedApiKey.getValue(), foundApiKey.getValue());
    }

    @Test
    public void testFindApiKeyByHash() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        ApiKey foundApiKey = apiKeyService.findApiKeyByValue(savedApiKey.getValue());

        Assert.assertNotNull(foundApiKey);
        Assert.assertEquals(savedApiKey.getId(), foundApiKey.getId());
        Assert.assertEquals(savedApiKey.getDescription(), foundApiKey.getDescription());
        Assert.assertEquals(savedApiKey.isEnabled(), foundApiKey.isEnabled());
        Assert.assertEquals(savedApiKey.getValue(), foundApiKey.getValue());
    }

    @Test
    public void testFindInternalApiKeyByDescription() {
        String uniqueDescription = "Unique API Key Description for Search";
        ApiKeyInfo apiKeyInfo = createInternalApiKeyInfo(uniqueDescription);
        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);

        ApiKey foundApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, uniqueDescription);

        Assert.assertNotNull(foundApiKey);
        Assert.assertEquals(savedApiKey.getId(), foundApiKey.getId());
        Assert.assertEquals(uniqueDescription, foundApiKey.getDescription());
        Assert.assertEquals(savedApiKey.isEnabled(), foundApiKey.isEnabled());
        Assert.assertEquals(savedApiKey.getValue(), foundApiKey.getValue());
    }

    @Test
    public void testFindInternalApiKeyByDescription_whenTwoSameDescriptionExists_thenReturnFirst() {
        String uniqueDescription = "Unique API Key Description for Search";

        ApiKeyInfo apiKeyInfo = createInternalApiKeyInfo(uniqueDescription);
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);

        ApiKeyInfo apiKeyInfo2 = createInternalApiKeyInfo(uniqueDescription);
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo2);

        ApiKey foundApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, uniqueDescription);

        Assert.assertNotNull(foundApiKey);
    }

    @Test
    public void testFindInternalApiKeyByDescription_notFound() {
        ApiKey foundApiKey = apiKeyService.findInternalApiKeyByDescription(tenantId, "Non-existent description");
        Assert.assertNull(foundApiKey);
    }

    @Test
    public void testFindApiKeysByUserId() {
        int size = 3;
        for (int i = 0; i < size; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        PageLink pageLink = new PageLink(10);
        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, pageLink);

        Assert.assertNotNull(pageData);
        Assert.assertEquals(size, pageData.getData().size());
        Assert.assertEquals(size, pageData.getTotalElements());
    }

    @Test
    public void testRotateInternalApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo("Internal key for rotation");
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setInternal(true);
        apiKeyInfo.setPermissions(null);

        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        String originalValue = savedApiKey.getValue();
        Assert.assertNotNull(originalValue);

        ApiKey rotatedApiKey = apiKeyService.rotateInternalApiKey(TenantId.SYS_TENANT_ID, savedApiKey);

        Assert.assertNotNull(rotatedApiKey);
        Assert.assertEquals(savedApiKey.getId(), rotatedApiKey.getId());
        Assert.assertNotEquals(originalValue, rotatedApiKey.getValue());
        Assert.assertTrue(rotatedApiKey.isInternal());
        Assert.assertEquals(savedApiKey.getDescription(), rotatedApiKey.getDescription());
    }

    @Test
    public void testRotateRegularApiKey_shouldFail() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo("Regular API key");
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        Assert.assertFalse(savedApiKey.isInternal());

        assertThatThrownBy(() -> apiKeyService.rotateInternalApiKey(tenantId, savedApiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can't rotate non-internal API Key!");
    }

    @Test
    public void testDeleteApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        apiKeyService.deleteApiKey(tenantId, savedApiKey, false);

        ApiKey foundApiKey = apiKeyService.findApiKeyById(tenantId, savedApiKey.getId());
        Assert.assertNull(foundApiKey);
    }

    @Test
    public void testDeleteByTenantId() {
        for (int i = 0; i < 3; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        apiKeyService.deleteByTenantId(tenantId);

        PageLink pageLink = new PageLink(10);
        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, pageLink);

        Assert.assertNotNull(pageData);
        Assert.assertEquals(0, pageData.getData().size());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testDeleteByUserId() {
        int size = 3;
        for (int i = 0; i < size; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, new PageLink(10));
        Assert.assertNotNull(pageData);
        Assert.assertEquals(size, pageData.getData().size());
        Assert.assertEquals(size, pageData.getTotalElements());

        apiKeyService.deleteByUserId(tenantId, userId);

        pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, new PageLink(10));
        Assert.assertNotNull(pageData);
        Assert.assertEquals(0, pageData.getData().size());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testDeleteInternalApiKey_shouldFailWithoutForce() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo("Internal key for deletion test");
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setInternal(true);

        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);

        assertThatThrownBy(() -> apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, savedApiKey, false))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Cannot delete internal API Key!");

        ApiKey foundKey = apiKeyService.findApiKeyById(TenantId.SYS_TENANT_ID, savedApiKey.getId());
        Assert.assertNotNull(foundKey);

        apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, savedApiKey, true);
        foundKey = apiKeyService.findApiKeyById(TenantId.SYS_TENANT_ID, savedApiKey.getId());
        Assert.assertNull(foundKey);
    }

    private ApiKeyInfo createInternalApiKeyInfo(String description) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setInternal(true);
        return apiKeyInfo;
    }

    private ApiKeyInfo createApiKeyInfo(String description) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(true);
        return apiKeyInfo;
    }

}
