/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.audit;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.audit.AuditLogDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {JpaAuditLogDaoTest.Config.class})
public class JpaAuditLogDaoTest extends AbstractJpaDaoTest {
    List<AuditLog> auditLogList = new ArrayList<>();
    UUID tenantId;
    CustomerId customerId1;
    CustomerId customerId2;
    UserId userId1;
    UserId userId2;
    EntityId entityId1;
    EntityId entityId2;
    AuditLog neededFoundedAuditLog;
    @Autowired
    private AuditLogDao auditLogDao;

    static class Config {
        @Bean
        @Primary
        public AuditLogDao auditLogDao(AuditLogDao auditLogDao) {
            return Mockito.mock(AuditLogDao.class, AdditionalAnswers.delegatesTo(auditLogDao));
        }
    }

    @Before
    public void setUp() {
        setUpIds();
        for (int i = 0; i < 60; i++) {
            ActionType actionType = i % 2 == 0 ? ActionType.ADDED : ActionType.DELETED;
            CustomerId customerId = i % 4 == 0 ? customerId1 : customerId2;
            UserId userId = i % 6 == 0 ? userId1 : userId2;
            EntityId entityId = i % 10 == 0 ? entityId1 : entityId2;
            auditLogList.add(createAuditLog(i, actionType, customerId, userId, entityId));
        }
        assertEquals(auditLogList.size(), auditLogDao.find(TenantId.fromUUID(tenantId)).size());
        neededFoundedAuditLog = auditLogList.get(0);
        assertNotNull(neededFoundedAuditLog);
    }

    private void setUpIds() {
        tenantId = Uuids.timeBased();
        customerId1 = new CustomerId(Uuids.timeBased());
        customerId2 = new CustomerId(Uuids.timeBased());
        userId1 = new UserId(Uuids.timeBased());
        userId2 = new UserId(Uuids.timeBased());
        entityId1 = new DeviceId(Uuids.timeBased());
        entityId2 = new DeviceId(Uuids.timeBased());
    }

    @After
    public void tearDown() {
        for (AuditLog auditLog : auditLogList) {
            auditLogDao.removeById(TenantId.fromUUID(tenantId), auditLog.getUuidId());
        }
        auditLogList.clear();
    }

    @Test
    public void testFindById() {
        AuditLog foundedAuditLogById = auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId());
        checkFoundedAuditLog(foundedAuditLogById);
    }
    
    @Test
    public void testFindByIdAsync() throws ExecutionException, InterruptedException, TimeoutException {
        AuditLog foundedAuditLogById = auditLogDao
                .findByIdAsync(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId()).get(30, TimeUnit.SECONDS);
        checkFoundedAuditLog(foundedAuditLogById);
    }

    private void checkFoundedAuditLog(AuditLog foundedAuditLogById) {
        assertNotNull(foundedAuditLogById);
        assertEquals(neededFoundedAuditLog, foundedAuditLogById);
    }

    @Test
    public void testFindAuditLogsByTenantId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantId(tenantId,
                List.of(ActionType.ADDED),
                new TimePageLink(40)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 30);
    }

    @Test
    public void testFindAuditLogsByTenantIdAndCustomerId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId,
                customerId1,
                List.of(ActionType.ADDED),
                new TimePageLink(20)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 15);
    }

    @Test
    public void testFindAuditLogsByTenantIdAndUserId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId,
                userId1,
                List.of(ActionType.ADDED),
                new TimePageLink(20)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 10);
    }

    @Test
    public void testFindAuditLogsByTenantIdAndEntityId() {
        List<AuditLog> foundedAuditLogs = auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId,
                entityId1,
                List.of(ActionType.ADDED),
                new TimePageLink(10)).getData();
        checkFoundedAuditLogsList(foundedAuditLogs, 6);
    }

    @Test
    public void testDeleteAuditLogWithTransactionalOk() throws Exception {
        ActionType actionType = ActionType.ADDED;
        CustomerId customerId = customerId1;
        UserId userId = userId1;
        EntityId entityId = entityId1;
        neededFoundedAuditLog = createAuditLog(1, actionType, customerId, userId, entityId);
        AuditLog foundedAuditLogById = auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId());
        checkFoundedAuditLog(foundedAuditLogById);

        auditLogDao.removeById(TenantId.fromUUID(tenantId), foundedAuditLogById.getUuidId());

        Assert.isNull(auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId()),
                "RemoveById is not success!");
    }

    @Test
    public void testDeleteAuditLogWithTransactionalException() throws Exception {
        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(auditLogDao).removeById(any(), any());
        try {
            AuditLog foundedAuditLogById = auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId());
            checkFoundedAuditLog(foundedAuditLogById);

            final Throwable raisedException = catchThrowable(() -> auditLogDao.removeById(TenantId.fromUUID(tenantId), foundedAuditLogById.getUuidId()));
            assertThat(raisedException).isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("mock message");

        } finally {
            Mockito.reset(auditLogDao);
        }
        AuditLog foundedAuditLogById = auditLogDao.findById(TenantId.fromUUID(tenantId), neededFoundedAuditLog.getUuidId());
        checkFoundedAuditLog(foundedAuditLogById);
    }


    private void checkFoundedAuditLogsList(List<AuditLog> foundedAuditLogs, int neededSizeForFoundedList) {
        assertNotNull(foundedAuditLogs);
        assertEquals(neededSizeForFoundedList, foundedAuditLogs.size());
    }

    private AuditLog createAuditLog(int number, ActionType actionType, CustomerId customerId, UserId userId, EntityId entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(TenantId.fromUUID(tenantId));
        auditLog.setCustomerId(customerId);
        auditLog.setUserId(userId);
        auditLog.setEntityId(entityId);
        auditLog.setUserName("AUDIT_LOG_" + number);
        auditLog.setActionType(actionType);
        return auditLogDao.save(TenantId.fromUUID(tenantId), auditLog);
    }
}
