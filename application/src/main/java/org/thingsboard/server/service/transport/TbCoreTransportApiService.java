/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Service
@TbCoreComponent
public class TbCoreTransportApiService {
    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final TransportApiService transportApiService;
    private final StatsFactory statsFactory;

    @Value("${queue.transport_api.max_pending_requests:10000}")
    private int maxPendingRequests;
    @Value("${queue.transport_api.max_requests_timeout:10000}")
    private long requestTimeout;
    @Value("${queue.transport_api.request_poll_interval:25}")
    private int responsePollDuration;
    @Value("${queue.transport_api.max_callback_threads:100}")
    private int maxCallbackThreads;

    private ExecutorService transportCallbackExecutor;
    private TbQueueResponseTemplate<TbProtoQueueMsg<TransportApiRequestMsg>,
            TbProtoQueueMsg<TransportApiResponseMsg>> transportApiTemplate;

    public TbCoreTransportApiService(TbCoreQueueFactory tbCoreQueueFactory, TransportApiService transportApiService, StatsFactory statsFactory) {
        this.tbCoreQueueFactory = tbCoreQueueFactory;
        this.transportApiService = transportApiService;
        this.statsFactory = statsFactory;
    }

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = Executors.newWorkStealingPool(maxCallbackThreads);
        TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> producer = tbCoreQueueFactory.createTransportApiResponseProducer();
        TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> consumer = tbCoreQueueFactory.createTransportApiRequestConsumer();

        String key = StatsType.TRANSPORT.getName();
        MessagesStats queueStats = statsFactory.createMessagesStats(key);

        DefaultTbQueueResponseTemplate.DefaultTbQueueResponseTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> builder = DefaultTbQueueResponseTemplate.builder();
        builder.requestTemplate(consumer);
        builder.responseTemplate(producer);
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(transportCallbackExecutor);
        builder.handler(transportApiService);
        builder.stats(queueStats);
        transportApiTemplate = builder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        transportApiTemplate.init(transportApiService);
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
