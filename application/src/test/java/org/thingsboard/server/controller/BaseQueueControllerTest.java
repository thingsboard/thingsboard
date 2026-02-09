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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.stats.DefaultRuleEngineStatisticsService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.rule-engine.stats.max-error-message-length=100",
        "transport.http.enabled=true"
})
public class BaseQueueControllerTest extends AbstractControllerTest {

    @Autowired
    private RuleEngineStatisticsService ruleEngineStatisticsService;
    @Autowired
    private StatsFactory statsFactory;
    @Autowired
    private QueueStatsService queueStatsService;
    @MockitoSpyBean
    private TimeseriesDao timeseriesDao;
    @MockitoSpyBean
    private PartitionService partitionService;
    @MockitoSpyBean
    private TimeseriesService timeseriesService;
    @MockitoSpyBean
    private ActorSystemContext actorSystemContext;

    @Test
    public void testQueueWithServiceTypeRE() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName("qwerty");
        queue.setTopic("tb_rule_engine.qwerty");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        // create queue
        Queue queue2 = new Queue();
        queue2.setName("qwerty2");
        queue2.setTopic("tb_rule_engine.qwerty2");
        queue2.setPollInterval(25);
        queue2.setPartitions(10);
        queue2.setTenantId(TenantId.SYS_TENANT_ID);
        queue2.setConsumerPerPartition(false);
        queue2.setPackProcessingTimeout(2000);
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue2.setSubmitStrategy(submitStrategy);
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue2.setProcessingStrategy(processingStrategy);

        Queue savedQueue = doPost("/api/queues?serviceType=" + "TB-RULE-ENGINE", queue, Queue.class);
        Queue savedQueue2 = doPost("/api/queues?serviceType=" + "TB_RULE_ENGINE", queue2, Queue.class);

        PageLink pageLink = new PageLink(10);
        PageData<Queue> pageData;
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB-RULE-ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue.getUuidId())
                .andExpect(status().isOk());

        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue2.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testQueueStatsTtl() throws ThingsboardException {
        Queue queue = new Queue();
        queue.setName("Test-1");
        queue.setTenantId(TenantId.SYS_TENANT_ID);

        TbRuleEngineProcessingResult testProcessingResult = Mockito.mock(TbRuleEngineProcessingResult.class);
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportProtos.ToRuleEngineMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build());
        when(testProcessingResult.getSuccessMap()).thenReturn(Stream.generate(() -> msg)
                .limit(5).collect(Collectors.toConcurrentMap(m -> UUID.randomUUID(), m -> m)));
        when(testProcessingResult.getFailedMap()).thenReturn(Stream.generate(() -> msg)
                .limit(5).collect(Collectors.toConcurrentMap(m -> UUID.randomUUID(), m -> m)));
        when(testProcessingResult.getPendingMap()).thenReturn(new ConcurrentHashMap<>());
        RuleEngineException ruleEngineException = new RuleEngineException("Test Exception");
        when(testProcessingResult.getExceptionsMap()).thenReturn(new ConcurrentHashMap<>(Map.of(
                tenantId, ruleEngineException
        )));

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(new QueueKey(ServiceType.TB_RULE_ENGINE, queue), statsFactory);
        testStats.log(testProcessingResult, true);

        int queueStatsTtlDays = 14;
        int ruleEngineExceptionsTtlDays = 7;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setQueueStatsTtlDays(queueStatsTtlDays);
            profileConfiguration.setRuleEngineExceptionsTtlDays(ruleEngineExceptionsTtlDays);
        });
        ruleEngineStatisticsService.reportQueueStats(System.currentTimeMillis(), testStats);

        PageData<QueueStats> queueStatsList = queueStatsService.findByTenantId(tenantId, new PageLink(10));
        assertThat(queueStatsList.getData()).hasSize(1);
        QueueStats queueStats = queueStatsList.getData().get(0);
        assertThat(queueStats.getQueueName()).isEqualTo(queue.getName());

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(timeseriesDao).save(eq(tenantId), eq(queueStats.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.SUCCESSFUL_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        verify(timeseriesDao).save(eq(tenantId), eq(queueStats.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.FAILED_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        assertThat(ttlCaptor.getAllValues()).allSatisfy(usedTtl -> {
            assertThat(usedTtl).isEqualTo(TimeUnit.DAYS.toSeconds(queueStatsTtlDays));
        });

        verify(timeseriesDao).save(eq(tenantId), eq(queueStats.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(DefaultRuleEngineStatisticsService.RULE_ENGINE_EXCEPTION) &&
                    tsKvEntry.getJsonValue().get().equals(ruleEngineException.toJsonString(0));
        }), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TimeUnit.DAYS.toSeconds(ruleEngineExceptionsTtlDays));
    }

    @Test
    public void testRuleEngineExceptionTruncation() {
        Queue queue = new Queue();
        queue.setName("Test-2");
        queue.setTenantId(TenantId.SYS_TENANT_ID);

        TbRuleEngineProcessingResult testProcessingResult = Mockito.mock(TbRuleEngineProcessingResult.class);
        when(testProcessingResult.getSuccessMap()).thenReturn(new ConcurrentHashMap<>());
        when(testProcessingResult.getFailedMap()).thenReturn(new ConcurrentHashMap<>());
        when(testProcessingResult.getPendingMap()).thenReturn(new ConcurrentHashMap<>());

        String largeExceptionMessage = RandomStringUtils.randomAlphabetic(150);
        RuleEngineException ruleEngineException = new RuleEngineException(largeExceptionMessage);
        when(testProcessingResult.getExceptionsMap()).thenReturn(new ConcurrentHashMap<>(Map.of(
                tenantId, ruleEngineException
        )));

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(new QueueKey(ServiceType.TB_RULE_ENGINE, queue), statsFactory);
        testStats.log(testProcessingResult, true);
        ruleEngineStatisticsService.reportQueueStats(System.currentTimeMillis(), testStats);

        AtomicReference<TsKvEntry> reExceptionTsKvEntryCaptor = new AtomicReference<>();
        verify(timeseriesDao).save(eq(tenantId), any(), argThat(tsKvEntry -> {
            if (tsKvEntry.getKey().equals(DefaultRuleEngineStatisticsService.RULE_ENGINE_EXCEPTION)) {
                reExceptionTsKvEntryCaptor.set(tsKvEntry);
                return true;
            }
            return false;
        }), anyLong());
        TsKvEntry reExceptionTsKvEntry = reExceptionTsKvEntryCaptor.get();

        String finalErrorMessage = JacksonUtil.toJsonNode(reExceptionTsKvEntry.getJsonValue().get()).get("message").asText();
        assertThat(finalErrorMessage).isEqualTo(largeExceptionMessage.substring(0, 100) + "...[truncated 50 symbols]");
    }

    @Test
    public void testMsgDuplicationToAllPartitions_fromTransport() throws Exception {
        loginSysAdmin();
        Queue queue = new Queue();
        queue.setName("RealTime");
        queue.setTopic("tb_rule_engine.real_time");
        queue.setPollInterval(25);
        int partitions = 12;
        queue.setPartitions(partitions);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setConsumerPerPartition(true);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);
        queue.setAdditionalInfo(JacksonUtil.newObjectNode()
                .put("duplicateMsgToAllPartitions", true));
        queue = saveQueue(queue);

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(partitionService.resolveAll(ServiceType.TB_RULE_ENGINE, "RealTime", tenantId, tenantId)).hasSize(partitions);
        });

        loginTenantAdmin();
        DeviceProfile deviceProfile = createDeviceProfile("realtime");
        deviceProfile.setDefaultQueueName(queue.getName());
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Device device = createDevice("test", deviceProfile.getName(), "test-token");

        JsonNode payload = JacksonUtil.newObjectNode()
                .put("test", "test");
        doPost("/api/v1/test-token/telemetry", payload).andExpect(status().isOk());

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(timeseriesService, times(partitions)).save(eq(tenantId), eq(device.getId()),
                    argThat(ts -> ts.size() == 1 && ts.get(0).getKey().equals("test")), anyLong());

            ArgumentCaptor<TbActorMsg> msgCaptor = ArgumentCaptor.forClass(TbActorMsg.class);
            verify(actorSystemContext, atLeastOnce()).tell(msgCaptor.capture());
            List<TbMsg> tbMsgs = msgCaptor.getAllValues().stream()
                    .map(actorMsg -> actorMsg instanceof QueueToRuleEngineMsg queueToRuleEngineMsg ? queueToRuleEngineMsg.getMsg() : null)
                    .filter(tbMsg -> tbMsg != null && tbMsg.getCorrelationId() != null && tbMsg.getInternalType() == TbMsgType.POST_TELEMETRY_REQUEST)
                    .toList();
            assertThat(tbMsgs).hasSize(partitions);
            UUID correlationId = tbMsgs.get(0).getCorrelationId();
            assertThat(tbMsgs).extracting(TbMsg::getCorrelationId).containsOnly(correlationId);
            assertThat(tbMsgs).extracting(TbMsg::getId).doesNotHaveDuplicates();
            assertThat(tbMsgs).extracting(TbMsg::getPartition).containsExactlyInAnyOrder(IntStream.range(0, partitions).boxed().toArray(Integer[]::new));
        });

        loginSysAdmin();
        ((ObjectNode) queue.getAdditionalInfo()).put("duplicateMsgToAllPartitions", false);
        queue = saveQueue(queue);

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(partitionService.resolveAll(ServiceType.TB_RULE_ENGINE, "RealTime", tenantId, tenantId)).hasSize(1);
        });

        doDelete("/api/queues/" + queue.getUuidId()).andExpect(status().isOk());
    }

    @Test
    public void testQueueWithReservedName() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName(DataConstants.CF_QUEUE_NAME);
        queue.setTopic("tb_rule_engine.calculated_fields");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        doPost("/api/queues?serviceType=" + "TB-RULE-ENGINE", queue)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(String.format("The queue name '%s' is not allowed. This name is reserved for internal use. Please choose a different name.", DataConstants.CF_QUEUE_NAME))));

        // create queue
        Queue queue2 = new Queue();
        queue2.setName(DataConstants.CF_STATES_QUEUE_NAME);
        queue2.setTopic("tb_rule_engine.calculated_fields");
        queue2.setPollInterval(25);
        queue2.setPartitions(10);
        queue2.setTenantId(TenantId.SYS_TENANT_ID);
        queue2.setConsumerPerPartition(false);
        queue2.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy2 = new SubmitStrategy();
        submitStrategy2.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue2.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy2 = new ProcessingStrategy();
        processingStrategy2.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy2.setRetries(3);
        processingStrategy2.setFailurePercentage(0.7);
        processingStrategy2.setPauseBetweenRetries(3);
        processingStrategy2.setMaxPauseBetweenRetries(5);
        queue2.setProcessingStrategy(processingStrategy);

        doPost("/api/queues?serviceType=" + "TB-RULE-ENGINE", queue2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(String.format("The queue name '%s' is not allowed. This name is reserved for internal use. Please choose a different name.", DataConstants.CF_STATES_QUEUE_NAME))));
    }

    private Queue saveQueue(Queue queue) {
        return doPost("/api/queues?serviceType=TB_RULE_ENGINE", queue, Queue.class);
    }

}
