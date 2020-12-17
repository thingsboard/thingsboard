/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TbMsgPackProcessingContext {

    private final String queueName;
    private final TbRuleEngineSubmitStrategy submitStrategy;
    @Getter
    private final boolean profilerEnabled;
    private final AtomicInteger pendingCount;
    private final CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> successMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> failedMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<TenantId, RuleEngineException> exceptionsMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, RuleNodeInfo> lastRuleNodeMap = new ConcurrentHashMap<>();

    public TbMsgPackProcessingContext(String queueName, TbRuleEngineSubmitStrategy submitStrategy) {
        this.queueName = queueName;
        this.submitStrategy = submitStrategy;
        this.profilerEnabled = log.isDebugEnabled();
        this.pendingMap = submitStrategy.getPendingMap();
        this.pendingCount = new AtomicInteger(pendingMap.size());
    }

    public boolean await(long packProcessingTimeout, TimeUnit milliseconds) throws InterruptedException {
        boolean success = processingTimeoutLatch.await(packProcessingTimeout, milliseconds);
        if (!success && profilerEnabled) {
            msgProfilerMap.values().forEach(this::onTimeout);
        }
        return success;
    }

    public void onSuccess(UUID id) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            successMap.put(id, msg);
            submitStrategy.onSuccess(id);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    public void onFailure(TenantId tenantId, UUID id, RuleEngineException e) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            failedMap.put(id, msg);
            exceptionsMap.putIfAbsent(tenantId, e);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    private final ConcurrentHashMap<UUID, TbMsgProfilerInfo> msgProfilerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TbRuleNodeProfilerInfo> ruleNodeProfilerMap = new ConcurrentHashMap<>();

    public void onProcessingStart(UUID id, RuleNodeInfo ruleNodeInfo) {
        lastRuleNodeMap.put(id, ruleNodeInfo);
        if (profilerEnabled) {
            msgProfilerMap.computeIfAbsent(id, TbMsgProfilerInfo::new).onStart(ruleNodeInfo.getRuleNodeId());
            ruleNodeProfilerMap.putIfAbsent(ruleNodeInfo.getRuleNodeId().getId(), new TbRuleNodeProfilerInfo(ruleNodeInfo));
        }
    }

    public void onProcessingEnd(UUID id, RuleNodeId ruleNodeId) {
        if (profilerEnabled) {
            long processingTime = msgProfilerMap.computeIfAbsent(id, TbMsgProfilerInfo::new).onEnd(ruleNodeId);
            if (processingTime > 0) {
                ruleNodeProfilerMap.computeIfAbsent(ruleNodeId.getId(), TbRuleNodeProfilerInfo::new).record(processingTime);
            }
        }
    }

    public void onTimeout(TbMsgProfilerInfo profilerInfo) {
        Map.Entry<UUID, Long> ruleNodeInfo = profilerInfo.onTimeout();
        if (ruleNodeInfo != null) {
            ruleNodeProfilerMap.computeIfAbsent(ruleNodeInfo.getKey(), TbRuleNodeProfilerInfo::new).record(ruleNodeInfo.getValue());
        }
    }

    public RuleNodeInfo getLastVisitedRuleNode(UUID id) {
        return lastRuleNodeMap.get(id);
    }

    public void printProfilerStats() {
        if (profilerEnabled) {
            log.debug("Top Rule Nodes by max execution time:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingLong(TbRuleNodeProfilerInfo::getMaxExecutionTime).reversed()).limit(5)
                    .forEach(info -> log.debug("[{}][{}] max execution time: {}. {}", queueName, info.getRuleNodeId(), info.getMaxExecutionTime(), info.getLabel()));

            log.info("Top Rule Nodes by avg execution time:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingDouble(TbRuleNodeProfilerInfo::getAvgExecutionTime).reversed()).limit(5)
                    .forEach(info -> log.info("[{}][{}] avg execution time: {}. {}", queueName, info.getRuleNodeId(), info.getAvgExecutionTime(), info.getLabel()));

            log.info("Top Rule Nodes by execution count:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingInt(TbRuleNodeProfilerInfo::getExecutionCount).reversed()).limit(5)
                    .forEach(info -> log.info("[{}][{}] execution count: {}. {}", queueName, info.getRuleNodeId(), info.getExecutionCount(), info.getLabel()));
        }
    }
}
