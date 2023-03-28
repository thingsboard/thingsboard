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
package org.thingsboard.rule.engine.cache;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public abstract class TbAbstractCacheBasedRuleNodeTest<N extends TbAbstractCacheBasedRuleNode<C, ?>, C> {

    protected N node;
    protected C config;

    protected TbContext ctx;
    protected RuleNodeCacheService ruleNodeCacheService;

    protected abstract ThingsBoardThreadFactory getThreadFactory();

    protected abstract String getTickMsgType();

    protected abstract String getEntityIdsCacheKey();

    protected CountDownLatch awaitTellSelfLatch;
    protected ScheduledExecutorService executorService;

    protected void doInit() {
        executorService = Executors.newSingleThreadScheduledExecutor(getThreadFactory());

        ctx = mock(TbContext.class);
        ruleNodeCacheService = mock(RuleNodeCacheService.class);

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleNodeCacheService()).thenReturn(Optional.of(ruleNodeCacheService));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(getTickMsgType()), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
    }

    protected void test_given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyMsgIgnored() throws ExecutionException, InterruptedException, TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(false);
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgToProcess = createMsg(deviceId, System.currentTimeMillis());
        node.onMsg(ctx, msgToProcess);

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, never()).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));

        node.destroy();
    }

    protected void test_givenNonLocalEntity_whenInit_thenVerifyEntityIgnored() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(false);
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, never()).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());

        node.destroy();
    }

    protected void test_givenLocalEntityWithNoValuesInCache_whenInit_thenVerifyDoNothing() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, times(1)).getPartition();
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());

        node.destroy();
    }

    protected void test_givenPartitionThatAlreadyExists_whenOnPartitionChange_thenVerifyCheckCacheSkipped() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        // add partition to the partitions map ...
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // check that partition is present and cache check is skipped!
        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));

        node.destroy();
    }

    protected void test_givenNoEntitiesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.emptySet());

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ctx, never()).getTopicPartitionInfo(eq(deviceId));

        node.destroy();
    }

    protected void test_givenLocalEntityWithNoValuesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));

        node.destroy();
    }

    protected TbMsg createMsg(DeviceId deviceId, long ts) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(ts));
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

    protected TbMsg createMsg(DeviceId deviceId) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

}