// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.alarm.AlarmCommentSubType.COMMENT_DELETED;

@Slf4j
@ContextConfiguration(classes = {AlarmCommentControllerTest.Config.class})
@DaoSqlTest
public class AlarmCommentControllerTest extends AbstractControllerTest {

    protected final String CUSTOMER_ADMIN_EMAIL = "testadmincustomer@thingsboard.org";
    protected final String CUSTOMER_ADMIN_PASSWORD = "admincustomer";

    protected final String DIFFERENT_CUSTOMER_ADMIN_EMAIL = "testdiffadmincustomer@thingsboard.org";
    protected final String DIFFERENT_CUSTOMER_ADMIN_PASSWORD = "diffadmincustomer";

    protected final String SECOND_CUSTOMER_ADMIN_EMAIL = "testsecondadmincustomer@thingsboard.org";
    protected final String SECOND_CUSTOMER_ADMIN_PASSWORD = "secondadmincustomer";

    protected Device customerDevice;
    protected Alarm alarm;
    private Role role;
    private EntityGroup entityGroup;
    private GroupPermission groupPermission;
    private final String classNameAlarm = "ALARM";


    static class Config {
        @Bean
        @Primary
        public AlarmDao alarmDao(AlarmDao alarmDao) {
            return Mockito.mock(AlarmDao.class, AdditionalAnswers.delegatesTo(alarmDao));
        }
    }

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test alarm type")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);

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

        resetTokens();
    }

    @After
    public void teardown() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);
        loginSysAdmin();
        deleteDifferentTenant();
        clearCustomerAdminPermissionGroup();
    }

    @Test
    public void testCreateAlarmCommentViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    @Test
    public void testCreateAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginCustomerUser();

        AlarmComment alarmComment = AlarmComment.builder()
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode(RandomStringUtils.randomAlphanumeric(10))))
                .build();

        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testCreateAlarmCommentViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());
        Assert.assertEquals(AlarmCommentType.OTHER, createdComment.getType());

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    @Test
    public void testUpdateAlarmCommentViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    @Test
    public void testUpdateAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerUser();
        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testUpdateAlarmCommentViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    @Test
    public void testEditOthersAlarmCommentIsProhibited() throws Exception {
        loginCustomerAdministrator();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Second customer rewrite"));
        alarmComment.setComment(newComment);

        loginSecondCustomerAdministrator();
        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("User is not allowed to edit other user's comment")));

        loginTenantAdmin();
        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("User is not allowed to edit other user's comment")));
    }

    @Test
    public void testUpdateAlarmCommentViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(equalTo(msgErrorNoFound("Alarm", alarm.getId().toString()))));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testUpdateAlarmCommentViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        loginDifferentCustomerAdministrator();
        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testDeleteAlarmCommentViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        alarmComment.setType(AlarmCommentType.SYSTEM);
        alarmComment.setUserId(null);
        alarmComment.setComment(JacksonUtil.newObjectNode()
                .put("text", String.format(COMMENT_DELETED.getText(), savedCustomerAdministrator.getTitle()))
                .put("subtype", COMMENT_DELETED.name())
                .put("userName", savedCustomerAdministrator.getTitle()));
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, alarmComment);
    }

    @Test
    public void testDeleteAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerUser();
        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testDeleteOthersAlarmCommentIsAllowedForUserWithAlarmWritePermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerUser();
        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        loginSecondCustomerAdministrator();
        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        alarmComment.setType(AlarmCommentType.SYSTEM);
        alarmComment.setUserId(null);
        alarmComment.setComment(JacksonUtil.newObjectNode()
                .put("text", String.format(COMMENT_DELETED.getText(), savedSecondCustomerAdministrator.getTitle()))
                .put("subtype", COMMENT_DELETED.name())
                .put("userName", savedSecondCustomerAdministrator.getTitle()));
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, savedSecondCustomerAdministrator.getId(), SECOND_CUSTOMER_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, alarmComment);
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        Optional<AlarmCommentInfo> systemCommentOpt = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment" + "?page=0&pageSize=10", new TypeReference<PageData<AlarmCommentInfo>>() {
                }
        ).getData().stream().filter(alarmCommentInfo -> alarmCommentInfo.getType().equals(AlarmCommentType.SYSTEM)).findFirst();
        assertThat(systemCommentOpt).isPresent();
        AlarmCommentInfo systemComment = systemCommentOpt.get();

        assertThat(systemComment.getId()).isEqualTo(alarmComment.getId());
        assertThat(systemComment.getType()).isEqualTo(AlarmCommentType.SYSTEM);
        assertThat(systemComment.getComment().get("text").asText()).isEqualTo(String.format("Comment was deleted by user %s",
                TENANT_ADMIN_EMAIL));

        alarmComment.setType(AlarmCommentType.SYSTEM);
        alarmComment.setUserId(null);
        alarmComment.setComment(JacksonUtil.newObjectNode()
                .put("text", String.format(COMMENT_DELETED.getText(), TENANT_ADMIN_EMAIL))
                .put("subtype", COMMENT_DELETED.name())
                .put("userName", TENANT_ADMIN_EMAIL));
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, alarmComment);
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testDeleteAlarmCommentViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testFindAlarmCommentsViaCustomerUser() throws Exception {
        loginCustomerAdministrator();

        List<AlarmComment> createdAlarmComments = new LinkedList<>();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.secure().nextAlphanumeric(10))
            );
        }

        var response = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size,
                new TypeReference<PageData<AlarmCommentInfo>>() {}
        );
        var foundAlarmCommentInfos = response.getData();
        Assert.assertNotNull("Found pageData is null", foundAlarmCommentInfos);
        Assert.assertNotEquals(
                "Expected alarms are not found!",
                0, foundAlarmCommentInfos.size()
        );

        boolean allMatch = createdAlarmComments.stream()
                .allMatch(alarmComment -> foundAlarmCommentInfos.stream()
                        .map(AlarmCommentInfo::getComment)
                        .anyMatch(comment -> alarmComment.getComment().equals(comment))
                );
        Assert.assertTrue("Created alarm comment doesn't match any found!", allMatch);
    }

    @Test
    public void testFindAlarmCommentssViaDifferentCustomerUser() throws Exception {
        loginCustomerAdministrator();

        final int size = 10;
        List<AlarmComment> createdAlarmComments = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.secure().nextAlphanumeric(10))
            );
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + classNameAlarm)));

        loginDifferentCustomerAdministrator();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + classNameAlarm)));
    }

    @Test
    public void testShouldNotCreateOrUpdateSystemAlarmComment() throws Exception {
        loginTenantAdmin();

        AlarmComment alarmComment = AlarmComment.builder()
                .type(AlarmCommentType.SYSTEM)
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode("Acknowledged by tenant admin")))
                .build();
        AlarmComment created = doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment, AlarmComment.class);
        assertThat(created.getType()).isEqualTo(AlarmCommentType.OTHER);

        // acknowledge alarm to create system comment
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());

        Optional<AlarmCommentInfo> systemCommentOpt = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment" + "?page=0&pageSize=10", new TypeReference<PageData<AlarmCommentInfo>>() {
                }
        ).getData().stream().filter(alarmCommentInfo -> alarmCommentInfo.getType().equals(AlarmCommentType.SYSTEM)).findFirst();
        assertThat(systemCommentOpt).isPresent();
        AlarmCommentInfo systemComment = systemCommentOpt.get();

        // system comment can't be updated with other type
        systemComment.setType(AlarmCommentType.OTHER);
        doPost("/api/alarm/" + alarm.getId() + "/comment", systemComment).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("System alarm comment can't be updated!")));

        // system comment can't be updated with other text
        systemComment.setType(AlarmCommentType.SYSTEM);
        systemComment.setComment(JacksonUtil.newObjectNode().set("text", new TextNode("New system comment")));
        doPost("/api/alarm/" + alarm.getId() + "/comment", systemComment).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("System alarm comment can't be updated!")));
    }

    private AlarmComment createAlarmComment(AlarmId alarmId, String text) {
        AlarmComment alarmComment = AlarmComment.builder()
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode(text)))
                .build();

        return saveAlarmComment(alarmId, alarmComment);
    }

    private AlarmComment createAlarmComment(AlarmId alarmId) {
        return createAlarmComment(alarmId, "Please take a look");
    }

    private AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        alarmComment = doPost("/api/alarm/" + alarmId + "/comment", alarmComment, AlarmComment.class);
        Assert.assertNotNull(alarmComment);

        return alarmComment;
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

    private User savedCustomerAdministrator;
    private User savedDifferentCustomerAdministrator;
    private User savedSecondCustomerAdministrator;

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

    private void loginSecondCustomerAdministrator() throws Exception {
        if (savedSecondCustomerAdministrator == null) {
            savedSecondCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    customerId,
                    SECOND_CUSTOMER_ADMIN_EMAIL,
                    SECOND_CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedSecondCustomerAdministrator.getEmail(), SECOND_CUSTOMER_ADMIN_PASSWORD);
    }

    private void loginDifferentCustomerAdministrator() throws Exception {
        if (savedDifferentCustomerAdministrator == null) {
            if (differentCustomerId == null) {
                createDifferentCustomer();
            }

            savedDifferentCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    differentCustomerId,
                    DIFFERENT_CUSTOMER_ADMIN_EMAIL,
                    DIFFERENT_CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedDifferentCustomerAdministrator.getEmail(), DIFFERENT_CUSTOMER_ADMIN_PASSWORD);
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
}
