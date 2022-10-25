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
package org.thingsboard.server.dao.service;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
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
import org.thingsboard.server.dao.queue.QueueDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {BaseQueueServiceTest.Config.class})
public abstract class BaseQueueServiceTest extends AbstractServiceTest {

    private IdComparator<Queue> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;

    @Autowired
    protected QueueDao queueDao;

    static class Config {
        @Bean
        @Primary
        public QueueDao queueDao(QueueDao queueDao) {
            return Mockito.mock(QueueDao.class, AdditionalAnswers.delegatesTo(queueDao));
        }
    }

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setDefault(false);
        tenantProfile.setName("Isolated TB Rule Engine");
        tenantProfile.setDescription("Isolated TB Rule Engine tenant profile");
        tenantProfile.setIsolatedTbRuleEngine(true);

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName("Main");
        mainQueueConfiguration.setTopic("tb_rule_engine.main");
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
        tenant.setTenantProfileId(tenantProfileId);
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
        Queue queue = createQueue();
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

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyName() {
        Queue queue = createQueue ("", "tb_rule_engine.test");
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithInvalidName() {
        Queue queue = createQueue ("Test 1", "tb_rule_engine.test");
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyTopic() {
        Queue queue = createQueue ("Test", "");
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithInvalidTopic() {
        Queue queue = createQueue ("Test", "tb rule engine test");
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPollInterval() {
        Queue queue = createQueue();
        queue.setPollInterval(0);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPartitions() {
        Queue queue = createQueue();
        queue.setPartitions(0);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPackProcessingTimeout() {
        Queue queue = createQueue();
        queue.setPackProcessingTimeout(0);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategy() {
        Queue queue = createQueue();
        queue.setSubmitStrategy(null);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyProcessingStrategy() {
        Queue queue = createQueue();
        queue.setProcessingStrategy(null);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategyType() {
        Queue queue = createQueue();
        queue.getSubmitStrategy().setType(null);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategyBatchSize() {
        Queue queue = createQueue();
        queue.getSubmitStrategy().setType(SubmitStrategyType.BATCH);
        queue.getSubmitStrategy().setBatchSize(0);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyProcessingStrategyType() {
        Queue queue = createQueue();
        queue.getProcessingStrategy().setType(null);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyRetries() {
        Queue queue = createQueue();
        queue.getProcessingStrategy().setRetries(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyFailurePercentage() {
        Queue queue = createQueue();
        queue.getProcessingStrategy().setFailurePercentage(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyPauseBetweenRetries() {
        Queue queue = createQueue();
        queue.getProcessingStrategy().setPauseBetweenRetries(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithProcessingStrategyPauseBetweenRetriesBiggerThenMaxPauseBetweenRetries() {
        Queue queue = createQueue();
        queue.getProcessingStrategy().setPauseBetweenRetries(100);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNotIsolatedTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Not isolated tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        Queue queue = createQueue();
        queue.setTenantId(savedTenant.getId());
        try {
            queueService.saveQueue(queue);
        } finally {
            tenantService.deleteTenant(savedTenant.getId());
        }
    }

    @Test
    public void testUpdateQueue() {
        Queue queue = createQueue();
        Queue savedQueue = queueService.saveQueue(queue);

        Assert.assertNotNull(savedQueue);

        queue.setPollInterval(1000);

        queueService.saveQueue(savedQueue);

        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());

        Assert.assertEquals(savedQueue, foundQueue);
    }


    @Test
    public void testFindQueueById() {
        Queue savedQueue = savedQueue();
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testDeleteQueue() {
        Queue savedQueue = savedQueue();
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        queueService.deleteQueue(tenantId, savedQueue.getId());
        foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNull(foundQueue);
    }

    @Test
    public void testFindQueueByTenantIdAndName() {
        Queue savedQueue = savedQueue();
        Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, savedQueue.getName());

        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testFindQueuesByTenantId() {
        List<Queue> queues = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            queues.add(savedQueue("Test" + i, "tb_rule_engine.test" + i));
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
            if (queue.getName().equals("Main")) {
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

    @Test
    public void testDeleteOAuth2ClientRegistrationTemplateWithTransactionalOk() {
        Queue savedQueue = savedQueue("MOCK_Ok" , "tb_rule_engine.test");
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        queueService.deleteQueue(tenantId, savedQueue.getId());
        foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNull(foundQueue);
    }

    @Test
    public void testDeleteOAuth2ClientRegistrationTemplateWithTransactionalException() throws Exception {
        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(queueDao).removeById(any(), any());
        Queue savedQueue = savedQueue("MOCK_CONSTRAINT", "tb_rule_engine.test");
        try {
            final Throwable raisedException = catchThrowable(() -> queueService.deleteQueue(tenantId, savedQueue.getId()));
            assertThat(raisedException).isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("mock message");

            Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
            Assert.assertNotNull(foundQueue);
        } finally {
            Mockito.reset(queueDao);
            await("Waiting for Mockito.reset takes effect")
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        final Throwable raisedException = catchThrowable(() -> queueService.deleteQueue(tenantId, savedQueue.getId()));
                        return raisedException == null;
                    });
        }
    }

    private Queue createQueue(String... args) {
        String name = args.length == 2 ? args[0] : "Test";
        String topic = args.length == 2 ? args[1] : "tb_rule_engine.test";
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName(name);
        queue.setTopic(topic);
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        return queue;
    }

    private Queue savedQueue(String... args) {
        Queue  queue = args.length == 2 ? createQueue(args) : createQueue();
        Queue savedQueue = queueService.saveQueue(queue);
        return savedQueue;
    }

}
