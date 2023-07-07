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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.service.ttl.AuditLogsCleanUpService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AuditLogControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private AuditLogDao auditLogDao;
    @SpyBean
    private SqlPartitioningRepository partitioningRepository;
    @Autowired
    private AuditLogsCleanUpService auditLogsCleanUpService;

    @Value("#{${sql.audit_logs.partition_size} * 60 * 60 * 1000}")
    private long partitionDurationInMs;
    @Value("${sql.ttl.audit_logs.ttl}")
    private long auditLogsTtlInSec;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testAuditLogs() throws Exception {
        for (int i = 0; i < 178; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            doPost("/api/device", device, Device.class);
        }

        List<AuditLog> loadedAuditLogs = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(23);
        PageData<AuditLog> pageData;
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(178, loadedAuditLogs.size());

        loadedAuditLogs = new ArrayList<>();
        pageLink = new TimePageLink(23);
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs/customer/" + ModelConstants.NULL_UUID + "?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(178, loadedAuditLogs.size());

        loadedAuditLogs = new ArrayList<>();
        pageLink = new TimePageLink(23);
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs/user/" + tenantAdmin.getId().getId().toString() + "?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(178, loadedAuditLogs.size());
    }

    @Test
    public void testAuditLogs_byTenantIdAndEntityId() throws Exception {
        Device device = new Device();
        device.setName("Device name");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        for (int i = 0; i < 178; i++) {
            savedDevice.setName("Device name" + i);
            doPost("/api/device", savedDevice, Device.class);
        }

        List<AuditLog> loadedAuditLogs = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(23);
        PageData<AuditLog> pageData;
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs/entity/DEVICE/" + savedDevice.getId().getId() + "?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(179, loadedAuditLogs.size());
    }

    @Test
    public void whenSavingNewAuditLog_thenCheckAndCreatePartitionIfNotExists() {
        reset(partitioningRepository);
        AuditLog auditLog = createAuditLog(ActionType.LOGIN, tenantAdminUserId);
        verify(partitioningRepository).createPartitionIfNotExists(eq("audit_log"), eq(auditLog.getCreatedTime()), eq(partitionDurationInMs));

        List<Long> partitions = partitioningRepository.fetchPartitions("audit_log");
        assertThat(partitions).singleElement().satisfies(partitionStartTs -> {
            assertThat(partitionStartTs).isEqualTo(partitioningRepository.calculatePartitionStartTime(auditLog.getCreatedTime(), partitionDurationInMs));
        });
    }

    @Test
    public void whenCleaningUpAuditLogsByTtl_thenDropOldPartitions() {
        long oldAuditLogTs = LocalDate.of(2020, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long partitionStartTs = partitioningRepository.calculatePartitionStartTime(oldAuditLogTs, partitionDurationInMs);
        partitioningRepository.createPartitionIfNotExists("audit_log", oldAuditLogTs, partitionDurationInMs);
        List<Long> partitions = partitioningRepository.fetchPartitions("audit_log");
        assertThat(partitions).contains(partitionStartTs);

        auditLogsCleanUpService.cleanUp();
        partitions = partitioningRepository.fetchPartitions("audit_log");
        assertThat(partitions).doesNotContain(partitionStartTs);
        assertThat(partitions).allSatisfy(partitionsStart -> {
            long partitionEndTs = partitionsStart + partitionDurationInMs;
            assertThat(partitionEndTs).isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(auditLogsTtlInSec));
        });
    }

    @Test
    public void whenSavingAuditLogAndPartitionSaveErrorOccurred_thenSaveAuditLogAnyway() throws Exception {
        // creating partition bigger than sql.audit_logs.partition_size
        partitioningRepository.createPartitionIfNotExists("audit_log", System.currentTimeMillis(), TimeUnit.DAYS.toMillis(7));
        List<Long> partitions = partitioningRepository.fetchPartitions("audit_log");
        assertThat(partitions).size().isOne();
        partitioningRepository.cleanupPartitionsCache("audit_log", System.currentTimeMillis(), 0);

        assertDoesNotThrow(() -> {
            // expecting partition overlap error on partition save
            createAuditLog(ActionType.LOGIN, tenantAdminUserId);
        });
        assertThat(partitioningRepository.fetchPartitions("audit_log")).isEqualTo(partitions);
    }

    private AuditLog createAuditLog(ActionType actionType, EntityId entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setCustomerId(null);
        auditLog.setUserId(tenantAdminUserId);
        auditLog.setEntityId(entityId);
        auditLog.setUserName(tenantAdmin.getEmail());
        auditLog.setActionType(actionType);
        return auditLogDao.save(tenantId, auditLog);
    }

}
