/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.transport;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActorToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.kafka.TbNodeIdProvider;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 09.10.18.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "transport", value = "type", havingValue = "remote")
public class RemoteRuleEngineTransportService implements RuleEngineTransportService {

    @Value("${transport.remote.rule_engine.topic}")
    private String ruleEngineTopic;
    @Value("${transport.remote.notifications.topic}")
    private String notificationsTopic;
    @Value("${transport.remote.rule_engine.poll_interval}")
    private int pollDuration;
    @Value("${transport.remote.rule_engine.auto_commit_interval}")
    private int autoCommitInterval;

    @Value("${transport.remote.rule_engine.poll_records_pack_size}")
    private int pollRecordsPackSize;
    @Value("${transport.remote.rule_engine.max_poll_records_per_second}")
    private long pollRecordsPerSecond;
    @Value("${transport.remote.rule_engine.max_poll_records_per_minute}")
    private long pollRecordsPerMinute;

    @Autowired
    private TbKafkaSettings kafkaSettings;

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Autowired
    private ActorSystemContext actorContext;

    //TODO: completely replace this routing with the Kafka routing by partition ids.
    @Autowired
    private ClusterRoutingService routingService;
    @Autowired
    private ClusterRpcService rpcService;
    @Autowired
    private DataDecodingEncodingService encodingService;

    private TBKafkaConsumerTemplate<ToRuleEngineMsg> ruleEngineConsumer;
    private TBKafkaProducerTemplate<ToTransportMsg> notificationsProducer;

    private ExecutorService mainConsumerExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean stopped = false;

    @PostConstruct
    public void init() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<ToTransportMsg> notificationsProducerBuilder = TBKafkaProducerTemplate.builder();
        notificationsProducerBuilder.settings(kafkaSettings);
        notificationsProducerBuilder.encoder(new ToTransportMsgEncoder());

        notificationsProducer = notificationsProducerBuilder.build();
        notificationsProducer.init();

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<ToRuleEngineMsg> ruleEngineConsumerBuilder = TBKafkaConsumerTemplate.builder();
        ruleEngineConsumerBuilder.settings(kafkaSettings);
        ruleEngineConsumerBuilder.topic(ruleEngineTopic);
        ruleEngineConsumerBuilder.clientId("transport-" + nodeIdProvider.getNodeId());
        ruleEngineConsumerBuilder.groupId("tb-node");
        ruleEngineConsumerBuilder.autoCommit(true);
        ruleEngineConsumerBuilder.autoCommitIntervalMs(autoCommitInterval);
        ruleEngineConsumerBuilder.maxPollRecords(pollRecordsPackSize);
        ruleEngineConsumerBuilder.decoder(new ToRuleEngineMsgDecoder());

        ruleEngineConsumer = ruleEngineConsumerBuilder.build();
        ruleEngineConsumer.subscribe();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        LocalBucketBuilder builder = Bucket4j.builder();
        builder.addLimit(Bandwidth.simple(pollRecordsPerSecond, Duration.ofSeconds(1)));
        builder.addLimit(Bandwidth.simple(pollRecordsPerMinute, Duration.ofMinutes(1)));
        LocalBucket pollRateBucket = builder.build();
        BlockingBucket blockingPollRateBucket = pollRateBucket.asScheduler();

        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    ConsumerRecords<String, byte[]> records = ruleEngineConsumer.poll(Duration.ofMillis(pollDuration));
                    int recordsCount = records.count();
                    if (recordsCount > 0) {
                        while (!blockingPollRateBucket.tryConsume(recordsCount, TimeUnit.SECONDS.toNanos(5))) {
                            log.info("Rule Engine consumer is busy. Required tokens: [{}]. Available tokens: [{}].", recordsCount, pollRateBucket.getAvailableTokens());
                            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                        }
                        log.trace("Processing {} records", recordsCount);
                    }
                    records.forEach(record -> {
                        try {
                            ToRuleEngineMsg toRuleEngineMsg = ruleEngineConsumer.decode(record);
                            log.trace("Forwarding message to rule engine {}", toRuleEngineMsg);
                            if (toRuleEngineMsg.hasToDeviceActorMsg()) {
                                forwardToDeviceActor(toRuleEngineMsg.getToDeviceActorMsg());
                            }
                        } catch (Throwable e) {
                            log.warn("Failed to process the notification.", e);
                        }
                    });
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

    @Override
    public void process(String nodeId, DeviceActorToTransportMsg msg) {
        process(nodeId, msg, null, null);
    }

    @Override
    public void process(String nodeId, DeviceActorToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        String topic = notificationsTopic + "." + nodeId;
        UUID sessionId = new UUID(msg.getSessionIdMSB(), msg.getSessionIdLSB());
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setToDeviceSessionMsg(msg).build();
        log.trace("[{}][{}] Pushing session data to topic: {}", topic, sessionId, transportMsg);
        notificationsProducer.send(topic, sessionId.toString(), transportMsg, new QueueCallbackAdaptor(onSuccess, onFailure));
    }

    private void forwardToDeviceActor(TransportToDeviceActorMsg toDeviceActorMsg) {
        TransportToDeviceActorMsgWrapper wrapper = new TransportToDeviceActorMsgWrapper(toDeviceActorMsg);
        Optional<ServerAddress> address = routingService.resolveById(wrapper.getDeviceId());
        if (address.isPresent()) {
            log.trace("[{}] Pushing message to remote server: {}", address.get(), toDeviceActorMsg);
            rpcService.tell(encodingService.convertToProtoDataMessage(address.get(), wrapper));
        } else {
            log.trace("Pushing message to local server: {}", toDeviceActorMsg);
            actorContext.getAppActor().tell(wrapper, ActorRef.noSender());
        }
    }

    @PreDestroy
    public void destroy() {
        stopped = true;
        if (ruleEngineConsumer != null) {
            ruleEngineConsumer.unsubscribe();
        }
        if (mainConsumerExecutor != null) {
            mainConsumerExecutor.shutdownNow();
        }
    }

    private static class QueueCallbackAdaptor implements Callback {
        private final Runnable onSuccess;
        private final Consumer<Throwable> onFailure;

        QueueCallbackAdaptor(Runnable onSuccess, Consumer<Throwable> onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                if (onFailure != null) {
                    onFailure.accept(exception);
                }
            }
        }
    }

}
