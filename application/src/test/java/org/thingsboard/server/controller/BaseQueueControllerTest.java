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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.stats.DefaultRuleEngineStatisticsService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.rule-engine.stats.max-error-message-length=100"
})
public class BaseQueueControllerTest extends AbstractControllerTest {

    @Autowired
    private RuleEngineStatisticsService ruleEngineStatisticsService;
    @Autowired
    private StatsFactory statsFactory;
    @SpyBean
    private TimeseriesDao timeseriesDao;
    @Autowired
    private AssetService assetService;

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

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(queue, statsFactory);
        testStats.log(testProcessingResult, true);

        int queueStatsTtlDays = 14;
        int ruleEngineExceptionsTtlDays = 7;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setQueueStatsTtlDays(queueStatsTtlDays);
            profileConfiguration.setRuleEngineExceptionsTtlDays(ruleEngineExceptionsTtlDays);
        });
        ruleEngineStatisticsService.reportQueueStats(System.currentTimeMillis(), testStats);

        Asset serviceAsset = assetService.findAssetsByTenantIdAndType(tenantId, TB_SERVICE_QUEUE, new PageLink(100)).getData()
                .stream().filter(asset -> asset.getName().startsWith(queue.getName()))
                .findFirst().get();

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.SUCCESSFUL_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.FAILED_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        assertThat(ttlCaptor.getAllValues()).allSatisfy(usedTtl -> {
            assertThat(usedTtl).isEqualTo(TimeUnit.DAYS.toSeconds(queueStatsTtlDays));
        });

        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
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

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(queue, statsFactory);
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

}
