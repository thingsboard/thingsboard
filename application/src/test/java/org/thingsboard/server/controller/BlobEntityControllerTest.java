// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.service.security.permission.AccessControlService;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.BlobEntityController.BLOB_ENTITY_ID;

@DaoSqlTest
public class BlobEntityControllerTest extends AbstractControllerTest {


    protected final String CUSTOMER_ADMIN_EMAIL = "testadmincustomer@thingsboard.org";
    protected final String CUSTOMER_ADMIN_PASSWORD = "admincustomer";
    private final String data = "Hello thingsboard";
    @Autowired
    protected JpaExecutorService service;
    @Autowired
    protected AccessControlService accessControlService;
    @MockitoSpyBean
    BlobEntityService blobEntityService;
    private Role role;
    private EntityGroup entityGroup;
    private GroupPermission groupPermission;
    private String type;
    private User savedCustomerAdministrator;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setType(RoleType.GENERIC);
        role.setName("Test customer administrator");
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));

        this.role = doPost("/api/role", role, Role.class);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Test customer administrators");
        entityGroup.setType(EntityType.USER);
        entityGroup.setOwnerId(customerId);
        this.entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        GroupPermission groupPermission = new GroupPermission(
                tenantId,
                this.entityGroup.getId(),
                this.role.getId(),
                null,
                null,
                false
        );
        this.groupPermission =
                doPost("/api/groupPermission", groupPermission, GroupPermission.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteDifferentTenant();
        clearCustomerAdminPermissionGroup();
    }

    @Test
    public void testGetBlobEntityInfoById() throws Exception {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = createBlobEntityWithCustomerInfoRandomUUID();
        BlobEntityId blobEntityId = blobEntityWithCustomerInfo.getId();
        String blobEntityIdStr = blobEntityId.getId().toString();

        doReturn(blobEntityWithCustomerInfo).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(tenantId), Mockito.eq(blobEntityId));

        BlobEntityWithCustomerInfo foundBlobEntityWithCustomerInfo = doGet("/api/blobEntity/info/" + blobEntityIdStr, BlobEntityWithCustomerInfo.class);
        Assert.assertNotNull(foundBlobEntityWithCustomerInfo);
        Assert.assertEquals(blobEntityWithCustomerInfo, foundBlobEntityWithCustomerInfo);
    }

    @Test
    public void testGetBlobEntityInfoByIdNonExistent_ResultNotFound() throws Exception {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = createBlobEntityWithCustomerInfoRandomUUID();
        BlobEntityId blobEntityId = blobEntityWithCustomerInfo.getId();
        String blobEntityIdStr = blobEntityId.getId().toString();

        doGet("/api/blobEntity/info/" + blobEntityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Blob entity", blobEntityIdStr))));
    }

    @Test
    public void testGetBlobEntityInfoByIdViaDifferentTenant() throws Exception {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = createBlobEntityWithCustomerInfoRandomUUID();
        BlobEntityId blobEntityId = blobEntityWithCustomerInfo.getId();
        String blobEntityIdStr = blobEntityId.getId().toString();

        loginDifferentTenant();

        doReturn(blobEntityWithCustomerInfo).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(savedDifferentTenant.getId()), Mockito.eq(blobEntityId));

        doGet("/api/blobEntity/info/" + blobEntityIdStr)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "BLOB_ENTITY '" + blobEntityWithCustomerInfo.getName() + "'!")));

        deleteDifferentTenant();
    }

    @Test
    public void testDownloadBlobEntity() throws Exception {
        BlobEntity blobEntity = createBlobEntityRandomUUID();
        BlobEntityId blobEntityId = blobEntity.getId();
        doReturn(blobEntity).when(blobEntityService).findBlobEntityById(Mockito.eq(tenantId), Mockito.eq(blobEntityId));

        byte[] content = doGet("/api/blobEntity/" + blobEntityId.getId().toString() + "/download").andReturn().getResponse().getContentAsByteArray();
        Assert.assertEquals(data, new String(content));
    }

    @Test
    public void testDeleteBlobEntity() throws Exception {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = createBlobEntityWithCustomerInfoRandomUUID();
        BlobEntityId blobEntityId = blobEntityWithCustomerInfo.getId();
        String blobEntityIdStr = blobEntityId.getId().toString();

        doReturn(blobEntityWithCustomerInfo).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(tenantId), Mockito.eq(blobEntityId));
        doNothing().when(blobEntityService).deleteBlobEntity(Mockito.eq(tenantId), Mockito.eq(blobEntityId));

        Mockito.clearInvocations(tbClusterService, auditLogService);

        doDelete("/api/blobEntity/" + blobEntityIdStr)
                .andExpect(status().isOk());

        Mockito.verify(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(tenantId), Mockito.eq(blobEntityId));
        Mockito.verify(blobEntityService).deleteBlobEntity(Mockito.eq(tenantId), Mockito.eq(blobEntityId));

        testNotifyEntityOneTimeMsgToEdgeServiceNever(blobEntityWithCustomerInfo, blobEntityWithCustomerInfo.getId(),
                blobEntityWithCustomerInfo.getId(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.DELETED, blobEntityIdStr);
    }

    @Test
    public void testDeleteNonExistentBlobEntity_ResultNotFound() throws Exception {
        BlobEntityId blobEntityId = new BlobEntityId(UUID.randomUUID());
        String blobEntityIdStr = blobEntityId.getId().toString();

        Mockito.clearInvocations(tbClusterService, auditLogService);

        doDelete("/api/blobEntity/" + blobEntityIdStr)
                //.andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Blob entity", blobEntityIdStr))));

        testNotifyEntityNever(null, new BlobEntity());
    }

    @Test
    public void testDeleteBlobEntityParameterIsEmpty() throws Exception {
        Mockito.clearInvocations(tbClusterService, auditLogService);

        String blobEntityIdStrEmpty = " ";
        doDelete("/api/blobEntity/" + blobEntityIdStrEmpty)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter '" + BLOB_ENTITY_ID + "' can't be empty!")));

        testNotifyEntityNever(null, new BlobEntity());
    }

    @Test
    public void testDeleteBlobEntityParameterIsInvalid() throws Exception {
        Mockito.clearInvocations(tbClusterService, auditLogService);

        String blobEntityIdStrInvalid = "aa";
        doDelete("/api/blobEntity/" + blobEntityIdStrInvalid)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid UUID string: " + blobEntityIdStrInvalid)));

        testNotifyEntityNever(null, new BlobEntity());
    }

    @Test
    public void testGetBlobEntitiesTenantUser() throws Exception {
        long cntBlobEntityInfo = 143;
        int totalPage = 10;
        PageData<BlobEntityWithCustomerInfo> blobEntityWithCustomerInfos = pageDataBlobEntityInfo(cntBlobEntityInfo, totalPage, null);
        type = "report_Test_With_Type";
        TimePageLink pageLink = new TimePageLink(10, 0);
        doReturn(blobEntityWithCustomerInfos).when(blobEntityService).findBlobEntitiesByTenantIdAndType(Mockito.eq(tenantId), Mockito.eq(type), Mockito.eq(pageLink));

        var response = doGetTyped("/api/blobEntities?page=" + pageLink.getPage() + "&pageSize=" + pageLink.getPageSize() + "&type=" + type,
                new TypeReference<PageData<BlobEntityWithCustomerInfo>>() {
                });
        Assert.assertEquals(cntBlobEntityInfo, response.getData().size());
        Assert.assertEquals(pageLink.getPageSize(), response.getTotalPages());
        Assert.assertEquals(cntBlobEntityInfo, response.getTotalElements());

        int cntBlobEntityInfoWithoutType = 153;
        PageData<BlobEntityWithCustomerInfo> blobEntityWithCustomerInfosPageDataWithoutType = pageDataBlobEntityInfo(cntBlobEntityInfoWithoutType, totalPage, blobEntityWithCustomerInfos.getData());
        doReturn(blobEntityWithCustomerInfosPageDataWithoutType).when(blobEntityService).findBlobEntitiesByTenantId(Mockito.eq(tenantId), Mockito.eq(pageLink));

        response = doGetTyped("/api/blobEntities?page=" + pageLink.getPage() + "&pageSize=" + pageLink.getPageSize(),
                new TypeReference<>() {
                });
        long cntBlobEntityInfoAll = cntBlobEntityInfo + cntBlobEntityInfoWithoutType;
        Assert.assertEquals(cntBlobEntityInfoAll, response.getData().size());
        Assert.assertEquals(pageLink.getPageSize(), response.getTotalPages());
        Assert.assertEquals(cntBlobEntityInfoAll, response.getTotalElements());
    }

    @Test
    public void testGetBlobEntitiesCustomerWithPermission() throws Exception {

        loginCustomerAdministrator();

        long cntBlobEntityInfo = 143;
        int totalPage = 10;
        PageData<BlobEntityWithCustomerInfo> blobEntityWithCustomerInfos = pageDataBlobEntityInfo(cntBlobEntityInfo, totalPage, null);
        type = "report_Test_With_Type";
        TimePageLink pageLink = new TimePageLink(10, 0);
        doReturn(blobEntityWithCustomerInfos).when(blobEntityService).findBlobEntitiesByTenantIdAndCustomerIdAndType(Mockito.eq(tenantId), Mockito.eq(customerId), Mockito.eq(type), Mockito.eq(pageLink));

        var response = doGetTyped("/api/blobEntities?page=" + pageLink.getPage() + "&pageSize=" + pageLink.getPageSize() + "&type=" + type,
                new TypeReference<PageData<BlobEntityWithCustomerInfo>>() {
                });
        Assert.assertEquals(cntBlobEntityInfo, response.getData().size());
        Assert.assertEquals(pageLink.getPageSize(), response.getTotalPages());
        Assert.assertEquals(cntBlobEntityInfo, response.getTotalElements());

        int cntBlobEntityInfoWithoutType = 153;
        PageData<BlobEntityWithCustomerInfo> blobEntityWithCustomerInfosPageDataWithoutType = pageDataBlobEntityInfo(cntBlobEntityInfoWithoutType, totalPage, blobEntityWithCustomerInfos.getData());
        doReturn(blobEntityWithCustomerInfosPageDataWithoutType).when(blobEntityService).findBlobEntitiesByTenantIdAndCustomerId(Mockito.eq(tenantId), Mockito.eq(customerId), Mockito.eq(pageLink));

        response = doGetTyped("/api/blobEntities?page=" + pageLink.getPage() + "&pageSize=" + pageLink.getPageSize(),
                new TypeReference<>() {
                });
        long cntBlobEntityInfoAll = cntBlobEntityInfo + cntBlobEntityInfoWithoutType;
        Assert.assertEquals(cntBlobEntityInfoAll, response.getData().size());
        Assert.assertEquals(pageLink.getPageSize(), response.getTotalPages());
        Assert.assertEquals(cntBlobEntityInfoAll, response.getTotalElements());
    }

    @Test
    public void testGetBlobEntitiesWithDifferentCustomerNotAdministrator() throws Exception {
        loginDifferentCustomer();

        TimePageLink pageLink = new TimePageLink(10, 0);
        var response = doGetTyped("/api/blobEntities?page=" + pageLink.getPage()
                        + "&pageSize=" + pageLink.getPageSize(),
                new TypeReference<PageData<BlobEntityWithCustomerInfo>>() {
                });

        PageData<BlobEntityWithCustomerInfo> pageDataExpected = new PageData<>();
        Assert.assertEquals(pageDataExpected.getData().size(), response.getData().size());
        Assert.assertEquals(pageDataExpected.getTotalPages(), response.getTotalPages());
        Assert.assertEquals(pageDataExpected.getTotalElements(), response.getTotalElements());
    }

    @Test
    public void testBlobEntitiesByIds() throws Exception {
        long cntBlobEntityInfo = 145;
        List<BlobEntityInfo> blobEntityInfos = createStrBlobEntityInfos(cntBlobEntityInfo);
        List<BlobEntityId> blobEntityIds = blobEntityInfos
                .stream()
                .map(blobEntity -> blobEntity.getId())
                .collect(Collectors.toList());
        String[] blobEntityIdStrs = blobEntityIds
                .stream()
                .map(blobEntity -> blobEntity.getId().toString())
                .collect(Collectors.toList())
                .toArray(String[]::new);
        String strBlobEntityIds = String.join(",", blobEntityIdStrs);
        doReturn(Futures.immediateFuture(blobEntityInfos)).when(blobEntityService).findBlobEntityInfoByIdsAsync(Mockito.eq(tenantId), Mockito.eq(blobEntityIds));

        var response = doGetTyped("/api/blobEntities?blobEntityIds=" + strBlobEntityIds,
                new TypeReference<List>() {
                });
        Assert.assertEquals(blobEntityInfos.size(), response.size());
    }

    @Test
    public void testBlobEntitiesByIdsNonExistent_Result_Item_0() throws Exception {
        long cntBlobEntityInfo = 145;
        List<BlobEntityInfo> blobEntityInfos = createStrBlobEntityInfos(cntBlobEntityInfo);
        List<BlobEntityId> blobEntityIds = blobEntityInfos
                .stream()
                .map(blobEntity -> blobEntity.getId())
                .collect(Collectors.toList());
        String[] blobEntityIdStrs = blobEntityIds
                .stream()
                .map(blobEntity -> blobEntity.getId().toString())
                .collect(Collectors.toList())
                .toArray(String[]::new);
        String strBlobEntityIds = String.join(",", blobEntityIdStrs);

        var response = doGetTyped("/api/blobEntities?blobEntityIds=" + strBlobEntityIds,
                new TypeReference<List>() {
                });
        Assert.assertEquals(0, response.size());
    }

    private BlobEntityWithCustomerInfo createBlobEntityWithCustomerInfoRandomUUID() {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = new BlobEntityWithCustomerInfo();
        blobEntityWithCustomerInfo.setId(new BlobEntityId(UUID.randomUUID()));
        blobEntityWithCustomerInfo.setTenantId(tenantId);
        blobEntityWithCustomerInfo.setCustomerId(customerId);
        blobEntityWithCustomerInfo.setName("Test BlobInfo entity - " + blobEntityWithCustomerInfo.getId().getId().toString());
        blobEntityWithCustomerInfo.setType(type);
        return blobEntityWithCustomerInfo;
    }

    private List<BlobEntityWithCustomerInfo> createBlobEntityWithCustomerInfos(long cntBlobEntityInfo) {
        return Stream.generate(this::createBlobEntityWithCustomerInfoRandomUUID)
                .limit(cntBlobEntityInfo)
                .collect(Collectors.toList());
    }

    private BlobEntity createBlobEntityRandomUUID() {
        BlobEntity blobEntity = new BlobEntity();
        blobEntity.setId(new BlobEntityId(UUID.randomUUID()));
        blobEntity.setTenantId(tenantId);
        blobEntity.setCustomerId(customerId);
        blobEntity.setName("Test Blob entity - " + blobEntity.getId().getId().toString());
        blobEntity.setData(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
        blobEntity.setContentType("application/json");
        return blobEntity;
    }

    private PageData<BlobEntityWithCustomerInfo> pageDataBlobEntityInfo(long cntBlobEntityInfo, int totalPage, List<BlobEntityWithCustomerInfo> dopBlobEntityWithCustomerInfos) {
        List<BlobEntityWithCustomerInfo> blobEntityWithCustomerInfos = createBlobEntityWithCustomerInfos(cntBlobEntityInfo);
        if (dopBlobEntityWithCustomerInfos != null) blobEntityWithCustomerInfos.addAll(dopBlobEntityWithCustomerInfos);
        return new PageData<>(
                blobEntityWithCustomerInfos,
                totalPage,
                blobEntityWithCustomerInfos.size(),
                true);
    }

    private List<BlobEntityInfo> createStrBlobEntityInfos(long cntBlobEntityInfo) {
        return Stream.generate(this::createBlobEntityRandomUUID)
                .limit(cntBlobEntityInfo)
                .collect(Collectors.toList());
    }

    private void loginCustomerAdministrator() throws Exception {
        if (savedCustomerAdministrator == null) {
            savedCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    customerId,
                    CUSTOMER_ADMIN_EMAIL,
                    CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedCustomerAdministrator.getEmail(), CUSTOMER_ADMIN_PASSWORD);

    }


    private User createCustomerAdministrator(TenantId tenantId, CustomerId customerId, String email, String pass) throws Exception {
        loginTenantAdmin();

        User user = new User();
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setFirstName("customer");
        user.setLastName("admin");
        user.setAuthority(Authority.CUSTOMER_USER);

        user = createUser(user, pass, entityGroup.getId());
        customerAdminUserId = user.getId();
        resetTokens();

        return user;
    }

    private void clearCustomerAdminPermissionGroup() throws Exception {
        loginTenantAdmin();
        doDelete("/api/groupPermission/" + groupPermission.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityGroup.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/role/" + role.getUuidId())
                .andExpect(status().isOk());
    }
}