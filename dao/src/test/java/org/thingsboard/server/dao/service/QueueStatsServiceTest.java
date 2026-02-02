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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueStatsService;

import static org.assertj.core.api.Assertions.assertThat;


@DaoSqlTest
public class QueueStatsServiceTest extends AbstractServiceTest {

    @Autowired
    QueueStatsService queueStatsService;

    private TenantId tenantId;

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveQueueStats() {
        QueueStats queueStats = new QueueStats();
        queueStats.setTenantId(tenantId);
        String queueName = StringUtils.randomAlphabetic(8);
        queueStats.setQueueName(queueName);
        queueStats.setServiceId(StringUtils.randomAlphabetic(8));

        QueueStats savedQueueStats = queueStatsService.save(tenantId, queueStats);
        Assert.assertNotNull(savedQueueStats);
        Assert.assertNotNull(savedQueueStats.getId());
        Assert.assertTrue(savedQueueStats.getCreatedTime() > 0);
        Assert.assertEquals(queueStats.getTenantId(), savedQueueStats.getTenantId());
        Assert.assertEquals(savedQueueStats.getQueueName(), queueStats.getQueueName());

        QueueStats retrievedQueueStatsById = queueStatsService.findQueueStatsById(tenantId, savedQueueStats.getId());
        Assert.assertEquals(retrievedQueueStatsById.getQueueName(), queueName);

        String secondQueueName = StringUtils.randomAlphabetic(8);
        queueStats.setQueueName(secondQueueName);
        QueueStats savedQueueStats2 = queueStatsService.save(tenantId, queueStats);
        QueueStats retrievedQueueStatsById2 = queueStatsService.findQueueStatsById(tenantId, savedQueueStats2.getId());
        Assert.assertEquals(retrievedQueueStatsById2.getQueueName(), secondQueueName);

        PageData<QueueStats> queueStatsList = queueStatsService.findByTenantId(tenantId, new PageLink(10));
        Assert.assertEquals(2, queueStatsList.getData().size());
        assertThat(queueStatsList.getData()).containsOnly(retrievedQueueStatsById, retrievedQueueStatsById2);

        queueStatsService.deleteByTenantId(tenantId);
        QueueStats retrievedQueueStatsAfterDelete = queueStatsService.findQueueStatsById(tenantId, savedQueueStats.getId());
        Assert.assertNull(retrievedQueueStatsAfterDelete);
    }

    @Test
    public void testSaveWithNullQueueName() {
        QueueStats queueStats = new QueueStats();
        queueStats.setTenantId(tenantId);
        queueStats.setQueueName(null);
        queueStats.setServiceId(StringUtils.randomAlphabetic(8));

        Assertions.assertThrows(DataValidationException.class, () -> {
            queueStatsService.save(tenantId, queueStats);
        });
    }

    @Test
    public void testSaveWithNullServiceId() {
        QueueStats queueStats = new QueueStats();
        queueStats.setTenantId(tenantId);
        queueStats.setQueueName(StringUtils.randomAlphabetic(8));
        queueStats.setServiceId(null);

        Assertions.assertThrows(DataValidationException.class, () -> {
            queueStatsService.save(tenantId, queueStats);
        });
    }

    @Test
    public void testFindByTenantIdAndNameAndServiceId() {
        QueueStats queueStats = new QueueStats();
        queueStats.setTenantId(tenantId);
        queueStats.setQueueName(StringUtils.randomAlphabetic(8));
        queueStats.setServiceId(StringUtils.randomAlphabetic(8));
        QueueStats savedQueueStats = queueStatsService.save(tenantId, queueStats);

        QueueStats queueStats2 = new QueueStats();
        queueStats2.setTenantId(tenantId);
        queueStats2.setQueueName(StringUtils.randomAlphabetic(8));
        queueStats2.setServiceId(StringUtils.randomAlphabetic(8));
        queueStatsService.save(tenantId, queueStats2);

        QueueStats retrievedQueueStatsById = queueStatsService.findByTenantIdAndNameAndServiceId(tenantId, queueStats.getQueueName(), queueStats.getServiceId());
        assertThat(retrievedQueueStatsById).isEqualTo(savedQueueStats);
    }

}
