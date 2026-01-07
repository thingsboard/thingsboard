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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.service.ttl.AuditLogsCleanUpService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Slf4j
@DaoSqlTest
public class AuditLogControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private AuditLogDao auditLogDao;
    @Getter
    @SpyBean
    private SqlPartitioningRepository partitioningRepository;
    @SpyBean
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
        savedTenant = saveTenant(tenant);
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

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testAuditLogs() throws Exception {
        for (int i = 0; i < 11; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            doPost("/api/device", device, Device.class);
        }

        List<AuditLog> loadedAuditLogs = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(5);
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

        Assert.assertEquals(11 + 1, loadedAuditLogs.size());

        loadedAuditLogs = new ArrayList<>();
        pageLink = new TimePageLink(5);
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs/customer/" + ModelConstants.NULL_UUID + "?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(11 + 1, loadedAuditLogs.size());

        loadedAuditLogs = new ArrayList<>();
        pageLink = new TimePageLink(5);
        do {
            pageData = doGetTypedWithTimePageLink("/api/audit/logs/user/" + tenantAdmin.getId().getId().toString() + "?",
                    new TypeReference<PageData<AuditLog>>() {
                    }, pageLink);
            loadedAuditLogs.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(11 + 1, loadedAuditLogs.size());
    }

    @Test
    public void testAuditLogs_byTenantIdAndEntityId() throws Exception {
        Device device = new Device();
        device.setName("Device name");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        for (int i = 0; i < 11; i++) {
            savedDevice.setName("Device name" + i);
            savedDevice = doPost("/api/device", savedDevice, Device.class);
        }

        List<AuditLog> loadedAuditLogs = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(5);
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

        Assert.assertEquals(11 + 1, loadedAuditLogs.size());
    }

    @Test
    public void whenSavingNewAuditLog_thenCheckAndCreatePartitionIfNotExists() throws ParseException {
        long entityTs = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2024-01-01T01:43:11Z").getTime();
        reset(getPartitioningRepository());
        AuditLog auditLog = createAuditLog(ActionType.LOGIN, tenantAdminUserId, entityTs);
        verify(getPartitioningRepository()).createPartitionIfNotExists(eq("audit_log"), eq(auditLog.getCreatedTime()), eq(partitionDurationInMs));

        List<Long> partitions = getPartitioningRepository().fetchPartitions("audit_log");
        assertThat(partitions).contains(getPartitioningRepository().calculatePartitionStartTime(auditLog.getCreatedTime(), partitionDurationInMs));
    }

    @Test
    public void whenCleaningUpAuditLogsByTtl_thenDropOldPartitions() throws ParseException {

        final long oldAuditLogTs = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-10-01T00:00:00Z").getTime();
        final long currentTimeMillis = oldAuditLogTs + TimeUnit.SECONDS.toMillis(auditLogsTtlInSec) * 2;

        final long partitionStartTs = getPartitioningRepository().calculatePartitionStartTime(oldAuditLogTs, partitionDurationInMs);
        getPartitioningRepository().createPartitionIfNotExists("audit_log", oldAuditLogTs, partitionDurationInMs);
        List<Long> partitions = getPartitioningRepository().fetchPartitions("audit_log");
        assertThat(partitions).contains(partitionStartTs);

        willReturn(currentTimeMillis).given(auditLogsCleanUpService).getCurrentTimeMillis();
        auditLogsCleanUpService.cleanUp();

        partitions = getPartitioningRepository().fetchPartitions("audit_log");
        assertThat(partitions).as("partitions cleared").doesNotContain(partitionStartTs);
        assertThat(partitions).as("only newer partitions left").allSatisfy(partitionsStart -> {
            long partitionEndTs = partitionsStart + partitionDurationInMs;
            assertThat(partitionEndTs).isGreaterThan(currentTimeMillis - TimeUnit.SECONDS.toMillis(auditLogsTtlInSec));
        });
    }

    @Test
    public void whenSavingAuditLogAndPartitionSaveErrorOccurred_thenSaveAuditLogAnyway() throws Exception {
        // creating partition bigger than sql.audit_logs.partition_size
        long entityTs = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-04-29T07:43:11Z").getTime();
        //the partition 7 days is overlapping default partition size 1 day, use in the far past to not affect other tests
        getPartitioningRepository().createPartitionIfNotExists("audit_log", entityTs, TimeUnit.DAYS.toMillis(7));
        List<Long> partitions = getPartitioningRepository().fetchPartitions("audit_log");
        log.warn("entityTs [{}], fetched partitions {}", entityTs, partitions);
        assertThat(partitions).contains(ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-04-28T00:00:00Z").getTime());
        getPartitioningRepository().cleanupPartitionsCache("audit_log", entityTs, 0);

        assertDoesNotThrow(() -> {
            // expecting partition overlap error on partition save
            createAuditLog(ActionType.LOGIN, tenantAdminUserId, entityTs);
        });
        assertThat(getPartitioningRepository().fetchPartitions("audit_log"))
                .contains(ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-04-28T00:00:00Z").getTime());
    }

    private AuditLog createAuditLog(ActionType actionType, EntityId entityId, long entityTs) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setCustomerId(null);
        auditLog.setUserId(tenantAdminUserId);
        auditLog.setEntityId(entityId);
        auditLog.setUserName(tenantAdmin.getEmail());
        auditLog.setActionType(actionType);

        if (entityTs > 0) {
            UUID uuid = Uuids.startOf(entityTs);
            auditLog.setId(new AuditLogId(uuid));
            auditLog.setCreatedTime(Uuids.unixTimestamp(uuid));
        }

        return auditLogDao.save(tenantId, auditLog);
    }

}
