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

import akka.actor.ActorRef;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueProvider;
import org.thingsboard.server.queue.util.TbMonolithOrRuleEngineComponent;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbMonolithOrRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineMsg, ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    @Value("${queue.rule_engine.poll_interval}")
    private long pollDuration;
    @Value("${queue.rule_engine.pack_processing_timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule_engine.stats.enabled:false}")
    private boolean statsEnabled;

    private final TbCoreConsumerStats stats = new TbCoreConsumerStats();
    private final TbRuleEngineProcessingStrategyFactory factory;

    public DefaultTbRuleEngineConsumerService(TbRuleEngineProcessingStrategyFactory factory, TbRuleEngineQueueProvider tbRuleEngineQueueProvider,
                                              ActorSystemContext actorContext, DataDecodingEncodingService encodingService) {
        super(actorContext, encodingService,
                tbRuleEngineQueueProvider.getToRuleEngineMsgConsumer(), tbRuleEngineQueueProvider.getToRuleEngineNotificationsMsgConsumer());
        this.factory = factory;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-consumer", "tb-rule-engine-notifications-consumer");
        this.factory.newInstance();
    }

    @Override
    protected void launchMainConsumer() {
        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = mainConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    TbRuleEngineProcessingStrategy strategy = factory.newInstance();
                    TbRuleEngineProcessingDecision decision = null;
                    boolean firstAttempt = true;
                    while (!stopped && (firstAttempt || !decision.isCommit())) {
                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> allMap;
                        if (firstAttempt) {
                            allMap = msgs.stream().collect(
                                    Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                            firstAttempt = false;
                        } else {
                            allMap = decision.getReprocessMap();
                        }
                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap = new ConcurrentHashMap<>();
                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failedMap = new ConcurrentHashMap<>();

                        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                        allMap.forEach((id, msg) -> {
                            log.info("[{}] Creating main callback for message: {}", id, msg.getValue());
                            TbMsgCallback callback = new MsgPackCallback<>(id, processingTimeoutLatch, allMap, successMap, failedMap);
                            try {
                                ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
                                TenantId tenantId = new TenantId(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
                                if (toRuleEngineMsg.getTbMsg() != null && !toRuleEngineMsg.getTbMsg().isEmpty()) {
                                    forwardToRuleEngineActor(tenantId, toRuleEngineMsg.getTbMsg(), callback);
                                } else {
                                    callback.onSuccess();
                                }
                            } catch (Throwable e) {
                                callback.onFailure(e);
                            }
                        });

                        boolean timeout = false;
                        if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                            timeout = true;
                        }
                        decision = strategy.analyze(new TbRuleEngineProcessingResult(timeout, allMap, successMap, failedMap));
                    }
                    mainConsumer.commit();
                } catch (Exception e) {
                    log.warn("Failed to process messages from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        });
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbMsgCallback callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.getComponentLifecycleMsg() != null && !nfMsg.getComponentLifecycleMsg().isEmpty()) {
            Optional<TbActorMsg> actorMsg = encodingService.decode(nfMsg.getComponentLifecycleMsg().toByteArray());
            if (actorMsg.isPresent()) {
                log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                actorContext.getAppActor().tell(actorMsg.get(), ActorRef.noSender());
            }
            callback.onSuccess();
        } else {
            callback.onSuccess();
        }
    }

    private void forwardToRuleEngineActor(TenantId tenantId, ByteString tbMsgData, TbMsgCallback callback) {
        TbMsg tbMsg = TbMsg.fromBytes(tbMsgData.toByteArray(), callback);
        actorContext.getAppActor().tell(new QueueToRuleEngineMsg(tenantId, tbMsg), ActorRef.noSender());
        //TODO: 2.5 before release.
//        if (statsEnabled) {
//            stats.log(toDeviceActorMsg);
//        }
    }

    @Scheduled(fixedDelayString = "${queue.rule_engine.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
        }
    }

}
