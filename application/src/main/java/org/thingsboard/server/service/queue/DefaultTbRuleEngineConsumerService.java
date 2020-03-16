/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.provider.TbCoreQueueProvider;
import org.thingsboard.server.provider.TbRuleEngineQueueProvider;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-rule-engine'")
@Slf4j
public class DefaultTbRuleEngineConsumerService implements TbRuleEngineConsumerService {


    @Value("${queue.rule_engine.poll_interval}")
    private long pollDuration;
    @Value("${queue.rule_engine.pack_processing_timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule_engine.stats.enabled:false}")
    private boolean statsEnabled;

    private final ActorSystemContext actorContext;
    private final TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;
    private final TbCoreConsumerStats stats = new TbCoreConsumerStats();
    private volatile ExecutorService mainConsumerExecutor;
    private volatile boolean stopped = false;

    public DefaultTbRuleEngineConsumerService(TbRuleEngineQueueProvider tbRuleEngineQueueProvider, ActorSystemContext actorContext) {
        this.consumer = tbRuleEngineQueueProvider.getToRuleEngineMsgConsumer();
        this.actorContext = actorContext;
    }

    @PostConstruct
    public void init() {
        this.consumer.subscribe();
        this.mainConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-consumer"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs = consumer.poll(pollDuration);
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ackMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    ackMap.forEach((id, msg) -> {
                        TbMsgCallback callback = new MsgPackCallback<>(id, processingTimeoutLatch, ackMap);
                        try {
                            TransportProtos.ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
                            log.trace("Forwarding message to rule engine {}", toRuleEngineMsg);
                            if (toRuleEngineMsg.hasToRuleEngineMsg()) {
                                forwardToRuleEngineActor(toRuleEngineMsg.getToRuleEngineMsg(), callback);
                            } else {
                                callback.onSuccess();
                            }
                        } catch (Throwable e) {
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                        ackMap.forEach((id, msg) -> log.warn("[{}] Timeout to process message: {}", id, msg.getValue()));
                    }
                    consumer.commit();
                } catch (Exception e) {
                    log.warn("Failed to obtain messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        });
    }

    //TODO 2.5
    private void forwardToRuleEngineActor(TransportProtos.TransportToRuleEngineMsg toDeviceActorMsg, TbMsgCallback callback) {
//        if (statsEnabled) {
//            stats.log(toDeviceActorMsg);
//        }
//        actorContext.getAppActor().tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback), ActorRef.noSender());
    }

    @Scheduled(fixedDelayString = "${queue.core.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
        }
    }

    @PreDestroy
    public void destroy() {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (mainConsumerExecutor != null) {
            mainConsumerExecutor.shutdownNow();
        }
    }
}
