/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ContextConfiguration(classes = {AlarmControllerTest.Config.class})
@DaoSqlTest
public class AlarmControllerTest extends AbstractControllerTest {

    public static final String TEST_ALARM_TYPE = "Test";

    protected Device customerDevice;

    @Autowired
    private AlarmDao alarmDao;

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

        resetTokens();
    }

    @After
    public void teardown() throws Exception {
        loginSysAdmin();

        deleteDifferentTenant();
    }

    @Test
    public void testCreateAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityAllOneTime(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);
    }

    @Test
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityAllOneTime(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);
    }

    @Test
    public void testUpdateAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, updatedAlarm.getId(), updatedAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.UPDATED);
    }

    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED);

        alarm = updatedAlarm;
        alarm.setAcknowledged(true);
        alarm.setAckTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isAcknowledged());
        Assert.assertEquals(alarm.getAckTs(), updatedAlarm.getAckTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ACK);

        alarm = updatedAlarm;
        alarm.setCleared(true);
        alarm.setClearTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isCleared());
        Assert.assertEquals(alarm.getClearTs(), updatedAlarm.getClearTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR);

        alarm = updatedAlarm;
        alarm.setAssigneeId(tenantAdminUserId);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(tenantAdminUserId, updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        alarm = updatedAlarm;
        alarm.setAssigneeId(null);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertNull(updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testDeleteAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.DELETED);
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED);
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testDeleteAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testClearAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_CLEAR);
    }

    @Test
    public void testClearAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR);
    }

    @Test
    public void testAcknowledgeAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_ACK);
    }

    @Test
    public void testClearAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testClearAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testAssignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);
    }

    @Test
    public void testAssignAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testReassignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        logout();

        loginCustomerUser();
        Mockito.reset(tbClusterService, auditLogService);
        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + customerUserId.getId()).andExpect(status().isOk());

        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(customerUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_ASSIGNED);
    }

    @Test
    public void testUnassignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);
        doDelete("/api/alarm/" + alarm.getId() + "/assign").andExpect(status().isOk());
        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    public void testUnassignTenantAlarmViaCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        logout();
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);
        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doDelete("/api/alarm/" + alarm.getId() + "/assign").andExpect(status().isOk());
        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    public void testUnassignAlarmOnUserRemoving() throws Exception {
        loginTenantAdmin();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenantForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs);

        beforeAssignmentTs = System.currentTimeMillis();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());
        Thread.sleep(2);

        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs);
    }

    @Test
    public void testFindAlarmsViaCustomerUser() throws Exception {
        loginCustomerUser();

        List<Alarm> createdAlarms = new LinkedList<>();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createdAlarms.add(
                    createAlarm(TEST_ALARM_TYPE + i)
            );
        }

        var response = doGetTyped(
                "/api/alarm/" + EntityType.DEVICE + "/"
                        + customerDevice.getUuidId() + "?page=0&pageSize=" + size,
                new TypeReference<PageData<AlarmInfo>>() {
                }
        );
        var foundAlarmInfos = response.getData();
        Assert.assertNotNull("Found pageData is null", foundAlarmInfos);
        Assert.assertNotEquals(
                "Expected alarms are not found!",
                0, foundAlarmInfos.size()
        );

        boolean allMatch = createdAlarms.stream()
                .allMatch(alarm -> foundAlarmInfos.stream()
                        .map(Alarm::getType)
                        .anyMatch(type -> alarm.getType().equals(type))
                );
        Assert.assertTrue("Created alarm doesn't match any found!", allMatch);
    }

    @Test
    public void testFindAlarmsViaDifferentCustomerUser() throws Exception {
        loginCustomerUser();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createAlarm(TEST_ALARM_TYPE + i);
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + EntityType.DEVICE + "/"
                + customerDevice.getUuidId() + "?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindAlarmsViaPublicCustomer() throws Exception {
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

        Mockito.reset(tbClusterService, auditLogService);

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull("Saved alarm is null!", alarm);

        testNotifyEntityNeverMsgToEdgeServiceOneTime(alarm, alarm.getId(), tenantId, ActionType.ADDED);

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        PageData<AlarmInfo> pageData = doGetTyped(
                "/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=1", new TypeReference<PageData<AlarmInfo>>() {
                }
        );

        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarms are not found!", 0, pageData.getTotalElements());

        AlarmInfo alarmInfo = pageData.getData().get(0);
        boolean equals = alarm.getId().equals(alarmInfo.getId()) && alarm.getType().equals(alarmInfo.getType());
        Assert.assertTrue("Created alarm doesn't match the found one!", equals);
    }

    @Test
    public void testDeleteAlarmWithDeleteRelationsOk() throws Exception {
        loginCustomerUser();
        AlarmId alarmId = createAlarm("Alarm for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(customerDevice.getId(), alarmId, "/api/alarm/" + alarmId);
    }

    @Ignore
    @Test
    public void testDeleteAlarmExceptionWithRelationsTransactional() throws Exception {
        loginCustomerUser();
        AlarmId alarmId = createAlarm("Alarm for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(alarmDao, customerDevice.getId(), alarmId, "/api/alarm/" + alarmId);
    }

    private AlarmInfo createAlarm(String type) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(alarm, new Alarm(foundAlarm));

        return foundAlarm;
    }


    @Test
    public void testCreateAlarmWithOtherTenantsAssignee() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .assigneeId(savedDifferentTenantUser.getId())
                .build();

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

    @Test
    public void testCreateAlarmWithOtherCustomerAsAssignee() throws Exception {
        loginDifferentCustomer();
        loginCustomerUser();

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .assigneeId(differentCustomerUser.getId())
                .build();

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

}
