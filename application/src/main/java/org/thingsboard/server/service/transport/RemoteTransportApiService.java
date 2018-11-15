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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.kafka.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport", value = "type", havingValue = "remote")
public class RemoteTransportApiService {

    @Value("${transport.remote.transport_api.requests_topic}")
    private String transportApiRequestsTopic;
    @Value("${transport.remote.transport_api.max_pending_requests}")
    private int maxPendingRequests;
    @Value("${transport.remote.transport_api.request_timeout}")
    private long requestTimeout;
    @Value("${transport.remote.transport_api.request_poll_interval}")
    private int responsePollDuration;
    @Value("${transport.remote.transport_api.request_auto_commit_interval}")
    private int autoCommitInterval;

    @Autowired
    private TbKafkaSettings kafkaSettings;

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Autowired
    private TransportApiService transportApiService;

    private ExecutorService transportCallbackExecutor;

    private TbKafkaResponseTemplate<TransportApiRequestMsg, TransportApiResponseMsg> transportApiTemplate;

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TransportApiResponseMsg> responseBuilder = TBKafkaProducerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.encoder(new TransportApiResponseEncoder());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TransportApiRequestMsg> requestBuilder = TBKafkaConsumerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.topic(transportApiRequestsTopic);
        requestBuilder.clientId(nodeIdProvider.getNodeId());
        requestBuilder.groupId("tb-node");
        requestBuilder.autoCommit(true);
        requestBuilder.autoCommitIntervalMs(autoCommitInterval);
        requestBuilder.decoder(new TransportApiRequestDecoder());

        TbKafkaResponseTemplate.TbKafkaResponseTemplateBuilder
                <TransportApiRequestMsg, TransportApiResponseMsg> builder = TbKafkaResponseTemplate.builder();
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(transportCallbackExecutor);
        builder.handler(transportApiService);
        transportApiTemplate = builder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        transportApiTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (transportApiTemplate != null) {
            transportApiTemplate.stop();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
    }

}
