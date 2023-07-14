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
package org.thingsboard.server.service.queue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class QueueServiceTest extends AbstractControllerTest {

    @SpyBean
    private ActorSystemContext actorContext;
    @Autowired
    private TbQueueService tbQueueService;
    @Autowired
    private QueueService queueService;
    @SpyBean
    private PartitionService partitionService;
    @Autowired
    private TbDeviceProfileCache deviceProfileCache;

    @Before
    public void beforeEach() {
        Queue mainQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.MAIN_QUEUE_NAME);
        if (mainQueue == null) {
            mainQueue = new Queue(TenantId.SYS_TENANT_ID, getMainQueueConfig());
            tbQueueService.saveQueue(mainQueue);
        }

        Queue hpQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.HP_QUEUE_NAME);
        if (hpQueue == null) {
            hpQueue = new Queue(TenantId.SYS_TENANT_ID, getHighPriorityQueueConfig());
            tbQueueService.saveQueue(hpQueue);
        }
    }

    @Test
    public void testQueuesUpdateOnTenantProfileUpdate() throws Exception {
        loginTenantAdmin();
        DeviceProfile hpQueueProfile = createDeviceProfile("HighPriority profile");
        hpQueueProfile.setDefaultQueueName(DataConstants.HP_QUEUE_NAME);
        hpQueueProfile = doPost("/api/deviceProfile", hpQueueProfile, DeviceProfile.class);
        Device hpQueueDevice = createDevice("HP", hpQueueProfile.getName(), "HP");
        deviceProfileCache.evict(tenantId, hpQueueProfile.getId());

        DeviceProfile mainQueueProfile = createDeviceProfile("Main profile");
        mainQueueProfile.setDefaultQueueName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueProfile = doPost("/api/deviceProfile", mainQueueProfile, DeviceProfile.class);
        Device mainQueueDevice = createDevice("Main", mainQueueProfile.getName(), "Main");

        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTenantId()).get().isEqualTo(TenantId.SYS_TENANT_ID);
        });
        verifyUsedQueueAndMessage(DataConstants.MAIN_QUEUE_NAME, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTenantId()).get().isEqualTo(TenantId.SYS_TENANT_ID);
        });

        updateDefaultTenantProfile(tenantProfile -> {
            tenantProfile.setIsolatedTbRuleEngine(true);
            tenantProfile.getProfileData().setQueueConfiguration(List.of(
                    getMainQueueConfig()
            ));
        });

        verifyUsedQueueAndMessage(DataConstants.MAIN_QUEUE_NAME, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).endsWith("main");
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });

        updateDefaultTenantProfile(tenantProfile -> {
            tenantProfile.setIsolatedTbRuleEngine(true);
            tenantProfile.getProfileData().setQueueConfiguration(List.of(
                    getMainQueueConfig(), getHighPriorityQueueConfig()
            ));
        });

        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).endsWith("hp");
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
        verifyUsedQueueAndMessage(DataConstants.MAIN_QUEUE_NAME, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
    }

    private void verifyUsedQueueAndMessage(String queue, EntityId entityId, String msgType, Runnable action, Consumer<TopicPartitionInfo> tpiAssert) {
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, queue, tenantId, entityId);
                    tpiAssert.accept(tpi);
                });
        action.run();
        TbMsg tbMsg = awaitTbMsg(msg -> msg.getOriginator().equals(entityId)
                && msg.getType().equals(msgType), 10000);
        assertThat(tbMsg.getQueueName()).isEqualTo(queue);

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, queue, tenantId, entityId);
        tpiAssert.accept(tpi);
    }

    protected TbMsg awaitTbMsg(Predicate<TbMsg> predicate, int timeoutMillis) {
        AtomicReference<TbMsg> tbMsgCaptor = new AtomicReference<>();
        verify(actorContext, timeout(timeoutMillis).atLeastOnce()).tell(argThat(actorMsg -> {
            if (!(actorMsg instanceof QueueToRuleEngineMsg)) {
                return false;
            }
            TbMsg tbMsg = ((QueueToRuleEngineMsg) actorMsg).getMsg();
            if (predicate.test(tbMsg)) {
                tbMsgCaptor.set(tbMsg);
                return true;
            }
            return false;
        }));
        return tbMsgCaptor.get();
    }

    private TenantProfileQueueConfiguration getHighPriorityQueueConfig() {
        TenantProfileQueueConfiguration hpQueueConfig = new TenantProfileQueueConfiguration();
        hpQueueConfig.setName(DataConstants.HP_QUEUE_NAME);
        hpQueueConfig.setTopic(DataConstants.HP_QUEUE_TOPIC);
        hpQueueConfig.setPollInterval(25);
        hpQueueConfig.setPartitions(10);
        hpQueueConfig.setConsumerPerPartition(true);
        hpQueueConfig.setPackProcessingTimeout(2000);
        SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
        highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        highPriorityQueueSubmitStrategy.setBatchSize(100);
        hpQueueConfig.setSubmitStrategy(highPriorityQueueSubmitStrategy);
        ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
        highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
        highPriorityQueueProcessingStrategy.setRetries(0);
        highPriorityQueueProcessingStrategy.setFailurePercentage(0);
        highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
        highPriorityQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
        hpQueueConfig.setProcessingStrategy(highPriorityQueueProcessingStrategy);
        return hpQueueConfig;
    }

    private TenantProfileQueueConfiguration getMainQueueConfig() {
        TenantProfileQueueConfiguration mainQueue = new TenantProfileQueueConfiguration();
        mainQueue.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueue.setPollInterval(25);
        mainQueue.setPartitions(10);
        mainQueue.setConsumerPerPartition(true);
        mainQueue.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
        return mainQueue;
    }

}
