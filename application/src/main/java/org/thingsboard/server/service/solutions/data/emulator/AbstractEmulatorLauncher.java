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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public abstract class AbstractEmulatorLauncher<T extends HasId<? extends EntityId> & HasTenantId & HasName> {

    protected static final TbQueueCallback EMPTY_CALLBACK = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {

        }

        @Override
        public void onFailure(Throwable t) {

        }
    };
    protected final T entity;
    protected final EmulatorDefinition emulatorDefinition;
    protected final ExecutorService oldTelemetryExecutor;
    protected final TbClusterService tbClusterService;
    protected final PartitionService partitionService;
    protected final TbQueueProducerProvider tbQueueProducerProvider;
    protected final TbServiceInfoProvider serviceInfoProvider;
    protected final TelemetrySubscriptionService tsSubService;
    protected final long publishFrequency;

    private final Emulator emulator;
    private ScheduledFuture<?> scheduledFuture;

    public AbstractEmulatorLauncher(T entity,
                                    EmulatorDefinition emulatorDefinition,
                                    ExecutorService oldTelemetryExecutor,
                                    TbClusterService tbClusterService,
                                    PartitionService partitionService,
                                    TbQueueProducerProvider tbQueueProducerProvider,
                                    TbServiceInfoProvider serviceInfoProvider,
                                    TelemetrySubscriptionService tsSubService) throws Exception {
        this.entity = entity;
        this.emulatorDefinition = emulatorDefinition;
        this.oldTelemetryExecutor = oldTelemetryExecutor;
        this.tbClusterService = tbClusterService;
        this.partitionService = partitionService;
        this.tbQueueProducerProvider = tbQueueProducerProvider;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tsSubService = tsSubService;
        this.publishFrequency = TimeUnit.SECONDS.toMillis(Math.max(emulatorDefinition.getPublishFrequencyInSeconds(), 1));
        if (StringUtils.isEmpty(emulatorDefinition.getClazz())) {
            emulator = new BasicEmulator();
        } else {
            emulator = (Emulator) Class.forName(emulatorDefinition.getClazz()).getDeclaredConstructor().newInstance();
        }
        emulator.init(emulatorDefinition);
    }

    public CompletableFuture<Void> launch() {
        final long oldestTs = emulatorDefinition.getOldestTs(System.currentTimeMillis());
        CompletableFuture<Void> future = new CompletableFuture<>();
        oldTelemetryExecutor.submit(() -> {
            AtomicInteger pending = new AtomicInteger(1);
            try {
                if (emulator instanceof SimpleEmulator) {
                    if (oldestTs < (System.currentTimeMillis() - publishFrequency)) {
                        pushOldTelemetry(oldestTs, pending, future);
                    }
                } else if (emulator instanceof CustomEmulator customEmulator) {
                    Pair<Long, ObjectNode> telemetry = customEmulator.getNextValue();
                    while (telemetry != null) {
                        pending.incrementAndGet();
                        publishTelemetry(telemetry.getFirst(), telemetry.getSecond(), pending, future);
                        telemetry = customEmulator.getNextValue();
                    }
                }
            } catch (Exception e) {
                log.warn("Telemetry upload failed for {}", entity.getName(), e);
                future.completeExceptionally(e);
            } finally {
                if (pending.decrementAndGet() == 0 && !future.isDone()) {
                    log.trace("[{}] Telemetry emulation finished. Producer and all callbacks completed successfully", entity.getName());
                    future.complete(null);
                }
            }

            future.whenComplete((v, t) -> {
                try {
                    postProcessEntity(entity);
                } catch (Exception e) {
                    log.warn("Post-processing failed for {}", entity.getName(), e);
                }
            });

        });
        return future;
    }

    protected void postProcessEntity(T entity) {
    }

    private void pushOldTelemetry(long oldestTs, AtomicInteger pending, CompletableFuture<Void> future) throws InterruptedException {
        for (long ts = oldestTs; ts < System.currentTimeMillis(); ts += publishFrequency) {
            pending.incrementAndGet();
            publishTelemetry(ts, pending, future);
        }
    }

    private void publishTelemetry(long ts, AtomicInteger pending, CompletableFuture<Void> future) throws InterruptedException {
        ObjectNode values = ((SimpleEmulator) emulator).getValue(ts);
        publishTelemetry(ts, values, pending, future);
        Thread.sleep(emulatorDefinition.getPublishPauseInMillis());
    }

    public void stop() {
        scheduledFuture.cancel(true);
    }

    private void publishTelemetry(long ts, ObjectNode value, AtomicInteger pending, CompletableFuture<Void> future) {
        String msgData = JacksonUtil.toString(value);
        log.debug("[{}] Publishing telemetry: {}", entity.getName(), msgData);
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("ts", Long.toString(ts));
        TbMsg tbMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(entity.getId())
                .copyMetaData(md)
                .dataType(TbMsgDataType.JSON)
                .data(msgData)
                .build();

        TbQueueCallback callback = new TbQueueCallback() {

            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.debug("[{}] Successfully pushed message to Rule Engine for ts {}", entity.getName(), ts);
                if (pending.decrementAndGet() == 0 && !future.isDone()) {
                    log.trace("[{}] Completing emulation future from callback for msg with ts: {}", entity.getName(), ts);
                    future.complete(null);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Telemetry upload failed for ts {}", entity.getName(), ts, t);
                if (pending.decrementAndGet() == 0 && !future.isDone()) {
                    future.completeExceptionally(t);
                }
            }
        };

        tbClusterService.pushMsgToRuleEngine(entity.getTenantId(), entity.getId(), tbMsg, callback);
    }
}
