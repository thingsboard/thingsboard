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
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.alarm.AlarmCommentSubType.COMMENT_DELETED;

@Slf4j
@ContextConfiguration(classes = {AlarmCommentControllerTest.Config.class})
@DaoSqlTest
public class AlarmCommentControllerTest extends AbstractControllerTest {

    protected Device customerDevice;
    protected Alarm alarm;

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

        resetTokens();
    }

    @After
    public void teardown() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);
        loginSysAdmin();
        deleteDifferentTenant();
    }

    @Test
    public void testCreateAlarmCommentViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
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
    public void testUpdateAlarmCommentViaCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
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
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
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
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testDeleteAlarmСommentViaCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(JacksonUtil.newObjectNode()
                        .put("text", String.format(COMMENT_DELETED.getText(), CUSTOMER_USER_EMAIL))
                        .put("subtype", COMMENT_DELETED.name())
                        .put("userName", CUSTOMER_USER_EMAIL))
                .build();
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.DELETED_COMMENT, 1, expectedAlarmComment);
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(JacksonUtil.newObjectNode()
                        .put("text", String.format(COMMENT_DELETED.getText(), TENANT_ADMIN_EMAIL))
                        .put("subtype", COMMENT_DELETED.name())
                        .put("userName", TENANT_ADMIN_EMAIL))
                .build();
        testLogEntityActionEntityEqClass(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, expectedAlarmComment);
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testDeleteAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testFindAlarmCommentsViaCustomerUser() throws Exception {
        loginCustomerUser();

        List<AlarmComment> createdAlarmComments = new LinkedList<>();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
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
    public void testFindAlarmsViaDifferentCustomerUser() throws Exception {
        loginCustomerUser();

        final int size = 10;
        List<AlarmComment> createdAlarmComments = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
            );
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindAlarmCommentsViaPublicCustomer() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setName("Test Public Device");
        device.setLabel("Label");
        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        device = doPost("/api/customer/public/device/" + device.getUuidId(), Device.class);

        String publicId = device.getCustomerId().toString();

        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("Test")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        Mockito.reset(tbClusterService, auditLogService);
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        PageData<AlarmCommentInfo> pageData = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment" + "?page=0&pageSize=1", new TypeReference<PageData<AlarmCommentInfo>>() {}
        );

        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarms are not found!", 0, pageData.getTotalElements());

        AlarmCommentInfo alarmCommentInfo = pageData.getData().get(0);
        boolean equals = alarmComment.getId().equals(alarmCommentInfo.getId()) && alarmComment.getComment().equals(alarmCommentInfo.getComment());
        Assert.assertTrue("Created alarm doesn't match the found one!", equals);
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
}
