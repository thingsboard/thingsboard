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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaResponseTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.state.DeviceStateService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport", value = "type", havingValue = "remote")
public class RemoteTransportApiService {

    @Value("${transport.remote.transport_api.requests_topic}")
    private String transportApiRequestsTopic;
    @Value("${transport.remote.transport_api.responses_topic}")
    private String transportApiResponsesTopic;
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
    private DiscoveryService discoveryService;

    @Autowired
    private TransportApiService transportApiService;

    private ExecutorService transportCallbackExecutor;

    private TbKafkaResponseTemplate<TransportApiRequestMsg, TransportApiResponseMsg> transportApiTemplate;

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TransportApiResponseMsg> responseBuilder = TBKafkaProducerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.defaultTopic(transportApiResponsesTopic);
        responseBuilder.encoder(new TransportApiResponseEncoder());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TransportApiRequestMsg> requestBuilder = TBKafkaConsumerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.topic(transportApiRequestsTopic);
        requestBuilder.clientId(discoveryService.getNodeId());
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
