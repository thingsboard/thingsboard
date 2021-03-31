package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseQueueServiceTest extends AbstractServiceTest {

    private IdComparator<Queue> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;

    @Before
    public void before() {
        TenantProfileData isolatedTenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration profileConfiguration = new DefaultTenantProfileConfiguration();
        profileConfiguration.setMaxNumberOfQueues(10);
        profileConfiguration.setMaxNumberOfPartitionsPerQueue(10);
        isolatedTenantProfileData.setConfiguration(profileConfiguration);

        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setDefault(false);
        tenantProfile.setName("Isolated TB Rule Engine");
        tenantProfile.setDescription("Isolated TB Rule Engine tenant profile");
        tenantProfile.setIsolatedTbCore(false);
        tenantProfile.setIsolatedTbRuleEngine(true);
        tenantProfile.setProfileData(isolatedTenantProfileData);

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

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyName() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyTopic() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPoolInterval() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPartitions() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPackProcessingTimeout() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategy() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyProcessingStrategy() {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
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
            queueService.saveQueue(queue);
        } finally {
            tenantService.deleteTenant(savedTenant.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithExceededLimitPerTenant() {
        for (int i = 1; i <= 10; i++) {
            //main queue created automatically
            Queue queue = new Queue();
            queue.setTenantId(tenantId);
            queue.setName("Test" + i);
            queue.setTopic("tb_rule_engine.test" + i);
            queue.setPollInterval(25);
            queue.setPartitions(1);
            queue.setPackProcessingTimeout(2000);
            queue.setSubmitStrategy(createTestSubmitStrategy());
            queue.setProcessingStrategy(createTestProcessingStrategy());
            queueService.saveQueue(queue);
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

}
