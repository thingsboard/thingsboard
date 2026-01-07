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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DaoSqlTest
public class QueueServiceTest extends AbstractServiceTest {

    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    QueueService queueService;

    private IdComparator<Queue> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setDefault(false);
        tenantProfile.setName("Isolated TB Rule Engine");
        tenantProfile.setDescription("Isolated TB Rule Engine tenant profile");
        tenantProfile.setIsolatedTbRuleEngine(true);

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        TenantProfileData profileData = tenantProfile.getProfileData();
        profileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));
        tenantProfile.setProfileData(profileData);

        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Assert.assertNotNull(savedTenantProfile);
        tenantProfileId = savedTenantProfile.getId();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(tenantProfileId); //custom profile
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileId);
    }

    private ProcessingStrategy createTestProcessingStrategy() {
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(3);
        return processingStrategy;
    }

    private SubmitStrategy createTestSubmitStrategy() {
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(1000);
        return submitStrategy;
    }

    @Test
    public void testSaveQueue() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);

        Assert.assertNotNull(savedQueue);
        Assert.assertNotNull(savedQueue.getId());
        Assert.assertTrue(savedQueue.getCreatedTime() > 0);
        Assert.assertEquals(queue.getTenantId(), savedQueue.getTenantId());
        Assert.assertEquals(queue.getName(), queue.getName());

        savedQueue.setPollInterval(100);

        queueService.saveQueue(savedQueue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertEquals(foundQueue.getPollInterval(), savedQueue.getPollInterval());

        queueService.deleteQueue(tenantId, foundQueue.getId());
    }

    @Test
    public void testSaveQueueWithEmptyName() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithInvalidName() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test 1");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyTopic() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithInvalidTopic() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb rule engine test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyPollInterval() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyPartitions() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyPackProcessingTimeout() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptySubmitStrategy() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyProcessingStrategy() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptySubmitStrategyType() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.getSubmitStrategy().setType(null);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptySubmitStrategyBatchSize() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.getSubmitStrategy().setType(SubmitStrategyType.BATCH);
        queue.getSubmitStrategy().setBatchSize(0);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithEmptyProcessingStrategyType() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setType(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithNegativeProcessingStrategyRetries() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setRetries(-1);
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithNegativeProcessingStrategyFailurePercentage() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setFailurePercentage(-1);
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithNegativeProcessingStrategyPauseBetweenRetries() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setPauseBetweenRetries(-1);
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithProcessingStrategyPauseBetweenRetriesBiggerThenMaxPauseBetweenRetries() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setPauseBetweenRetries(100);
        Assertions.assertThrows(DataValidationException.class, () -> {
            queueService.saveQueue(queue);
        });
    }

    @Test
    public void testSaveQueueWithNotIsolatedTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Not isolated tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        Queue queue = new Queue();
        queue.setTenantId(savedTenant.getId());
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                queueService.saveQueue(queue);
            });
        } finally {
            tenantService.deleteTenant(savedTenant.getId());
        }
    }

    @Test
    public void testUpdateQueue() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);

        Assert.assertNotNull(savedQueue);

        queue.setPollInterval(1000);

        queueService.saveQueue(savedQueue);

        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());

        Assert.assertEquals(savedQueue, foundQueue);
    }


    @Test
    public void testFindQueueById() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testDeleteQueue() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        queueService.deleteQueue(tenantId, savedQueue.getId());
        foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNull(foundQueue);
    }

    @Test
    public void testFindQueueByTenantIdAndName() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, savedQueue.getName());

        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testFindQueuesByTenantId() {
        List<Queue> queues = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            Queue queue = new Queue();
            queue.setTenantId(tenantId);
            queue.setName("Test" + i);
            queue.setTopic("tb_rule_engine.test" + i);
            queue.setPollInterval(25);
            queue.setPartitions(1);
            queue.setPackProcessingTimeout(2000);
            queue.setSubmitStrategy(createTestSubmitStrategy());
            queue.setProcessingStrategy(createTestProcessingStrategy());

            queues.add(queueService.saveQueue(queue));
        }

        List<Queue> loadedQueues = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Queue> pageData = null;
        do {
            pageData = queueService.findQueuesByTenantId(tenantId, pageLink);
            loadedQueues.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        for (int i = 0; i < loadedQueues.size(); i++) {
            Queue queue = loadedQueues.get(i);
            if (queue.getName().equals(DataConstants.MAIN_QUEUE_NAME)) {
                loadedQueues.remove(queue);
                break;
            }
        }

        Collections.sort(queues, idComparator);
        Collections.sort(loadedQueues, idComparator);

        Assert.assertEquals(queues, loadedQueues);

        queueService.deleteQueuesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = queueService.findQueuesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

}
