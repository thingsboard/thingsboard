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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
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
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);
    }

    @Test
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
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
        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, updatedAlarm.getId(), updatedAlarm.getOriginator(),
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
        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED);

        alarm = updatedAlarm;
        alarm.setAcknowledged(true);
        alarm.setAckTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isAcknowledged());
        Assert.assertEquals(alarm.getAckTs(), updatedAlarm.getAckTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ACK, 1, 0, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setCleared(true);
        alarm.setClearTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isCleared());
        Assert.assertEquals(alarm.getClearTs(), updatedAlarm.getClearTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR, 1, 0, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setAssigneeId(tenantAdminUserId);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(tenantAdminUserId, updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED, 1, 0, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setAssigneeId(null);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertNull(updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_UNASSIGNED, 1, 0, 1);
        Mockito.reset(tbClusterService, auditLogService);

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

        testNotifyEntityAllOneTime(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_DELETE, alarm.getId());
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityAllOneTime(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_DELETE, alarm.getId());
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
    public void testDeleteNonExistentAlarm() throws Exception {
        loginTenantAdmin();

        var nonExistentAlarmId = Uuids.timeBased();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + nonExistentAlarmId)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(is("Alarm with id [" + nonExistentAlarmId + "] is not found")));

        verifyNoInteractions(tbClusterService, auditLogService);
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);
        doDelete("/api/alarm/" + alarm.getId() + "/assign").andExpect(status().isOk());
        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
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

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    public void testUnassignTenantUserAlarmOnUserRemoving() throws Exception {
        loginDifferentTenant();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenantForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Device device = createDevice("Different tenant device", "default", "differentTenantTest");

        Alarm alarm = Alarm.builder()
                .type(TEST_ALARM_TYPE)
                .tenantId(savedDifferentTenant.getId())
                .originator(device.getId())
                .severity(AlarmSeverity.MAJOR)
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        alarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();

        long beforeAssignmentTs = System.currentTimeMillis();

        doPost("/api/alarm/" + alarmId.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);

        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);

        long afterAssignmentTs = System.currentTimeMillis();

        loginSysAdmin();

        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        loginDifferentTenant();

        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            AlarmInfo alarmInfo = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
            Assert.assertNotNull(alarmInfo);
            Assert.assertNull(alarmInfo.getAssigneeId());
            Assert.assertTrue(alarmInfo.getAssignTs() >= afterAssignmentTs);
        });
    }

    @Test
    public void testUnassignAlarmOnUserRemoving() throws Exception {
        loginDifferentTenant();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenantForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Device device = createDevice("Different tenant device", "default", "differentTenantTest");

        Alarm alarm = Alarm.builder()
                .type(TEST_ALARM_TYPE)
                .tenantId(savedDifferentTenant.getId())
                .originator(device.getId())
                .severity(AlarmSeverity.MAJOR)
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();
        alarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(alarm);
        long beforeAssignmentTs = System.currentTimeMillis();

        doPost("/api/alarm/" + alarmId.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);

        long afterAssignmentTs = System.currentTimeMillis();

        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            AlarmInfo alarmInfo = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
            Assert.assertNotNull(alarmInfo);
            Assert.assertNull(alarmInfo.getAssigneeId());
            Assert.assertTrue(alarmInfo.getAssignTs() >= afterAssignmentTs);
        });
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

        testNotifyEntityNever(alarm.getId(), alarm);

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
        return createAlarm(type, customerDevice.getId(), customerId);
    }

    private AlarmInfo createAlarm(String type, EntityId originatorId, CustomerId customerId) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(originatorId)
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

    @Test
    public void testSaveAlarmTypes() throws Exception {
        loginTenantAdmin();

        List<String> types = new ArrayList<>();

        for (int i = 1; i < 13; i++) {
            types.add(createAlarm(TEST_ALARM_TYPE + i).getType());
        }

        Device device = new Device();
        device.setName("Test device 2");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        for (int i = 1; i < 10; i++) {
            createAlarm(TEST_ALARM_TYPE + i);
        }

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .collect(Collectors.toList());

        Collections.sort(types);
        Collections.sort(foundTypes);

        Assert.assertEquals(types, foundTypes);
    }

    @Test
    public void testDeleteAlarmTypes() throws Exception {
        loginTenantAdmin();

        List<AlarmInfo> alarms = new ArrayList<>();

        for (int i = 1; i < 13; i++) {
            alarms.add(createAlarm(TEST_ALARM_TYPE + i));
        }

        Device device = new Device();
        device.setName("Test device 2");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        for (int i = 1; i < 14; i++) {
            alarms.add(createAlarm(TEST_ALARM_TYPE + i));
        }

        List<String> expectedTypes = alarms.stream().map(AlarmInfo::getType).distinct().sorted().collect(Collectors.toList());

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(expectedTypes, foundTypes);

        for (int i = 0; i < 12; i++) {
            doDelete("/api/alarm/" + alarms.get(i).getId()).andExpect(status().isOk());
        }

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(expectedTypes, foundTypes);

        doDelete("/api/alarm/" + alarms.get(12).getId()).andExpect(status().isOk());

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(12, foundTypes.size());

        for (int i = 13; i < alarms.size(); i++) {
            doDelete("/api/alarm/" + alarms.get(i).getId()).andExpect(status().isOk());
        }

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertTrue(foundTypes.isEmpty());
    }

    @Test
    public void testDeleteAlarmTypesAfterDeletingAlarmOriginator() throws Exception {
        loginTenantAdmin();

        List<Device> devices = new ArrayList<>();
        List<String> types = new ArrayList<>();

        for (int i = 0; i < 13; i++) {
            Device device = new Device();
            device.setName("Test_device_" + i);
            devices.add(device = doPost("/api/device", device, Device.class));
            types.add(createAlarm(TEST_ALARM_TYPE + i, device.getId(), null).getType());
        }

        devices.sort(Comparator.comparing(Device::getName));
        Collections.sort(types);

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(types, foundTypes);

        for (int i = 0; i < 12; i++) {
            doDelete("/api/device/" + devices.remove(0).getId()).andExpect(status().isOk());
            types.remove(0);
        }

        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> actualTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {})
                    .getData().stream().map(EntitySubtype::getType).toList();
            Assert.assertEquals(types.size(), actualTypes.size());
            Assert.assertEquals(types, actualTypes);
        });

        doDelete("/api/device/" + devices.get(0).getId()).andExpect(status().isOk());

        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> actualTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {})
                    .getData().stream().map(EntitySubtype::getType).sorted().toList();
            Assert.assertTrue(actualTypes.isEmpty());
        });
    }

}
