/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
public class TbKafkaResponseTemplate<Request, Response> extends AbstractTbKafkaTemplate {

    private final TBKafkaConsumerTemplate<Request> requestTemplate;
    private final TBKafkaProducerTemplate<Response> responseTemplate;
    private final TbKafkaHandler<Request, Response> handler;
    private final ConcurrentMap<UUID, String> pendingRequests;
    private final ExecutorService executor;
    private final int maxPendingRequests;

    private final long pollInterval;
    private volatile boolean stopped = false;
    //TODO:
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    @Builder
    public TbKafkaResponseTemplate(TBKafkaConsumerTemplate<Request> requestTemplate,
                                   TBKafkaProducerTemplate<Response> responseTemplate,
                                   TbKafkaHandler<Request, Response> handler,
                                   long pollInterval,
                                   int maxPendingRequests,
                                   ExecutorService executor) {
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.handler = handler;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.executor = executor;
    }

    public void init() {
        this.responseTemplate.init();
        requestTemplate.subscribe();
        executor.submit(() -> {
            while (!stopped) {
                if(pendingRequestCount.get() > maxPendingRequests){

                }
                //TODO: we need to protect from reading too much requests.
                ConsumerRecords<String, byte[]> requests = requestTemplate.poll(Duration.ofMillis(pollInterval));
                requests.forEach(request -> {
                    Header requestIdHeader = request.headers().lastHeader(TbKafkaSettings.REQUEST_ID_HEADER);
                    if (requestIdHeader == null) {
                        log.error("[{}] Missing requestId in header", request);
                        return;
                    }
                    UUID requestId = bytesToUuid(requestIdHeader.value());
                    if (requestId == null) {
                        log.error("[{}] Missing requestId in header and body", request);
                        return;
                    }
                    Header responseTopicHeader = request.headers().lastHeader(TbKafkaSettings.RESPONSE_TOPIC_HEADER);
                    if (responseTopicHeader == null) {
                        log.error("[{}] Missing response topic in header", request);
                        return;
                    }
                    String responseTopic = bytesToString(responseTopicHeader.value());
                    try {
                        Request decodedRequest = requestTemplate.decode(request);
                        executor.submit(() -> handler.handle(decodedRequest,
                                response -> reply(requestId, responseTopic, response),
                                e -> log.error("[{}] Failed to process the request: {}", requestId, request, e)));
                    } catch (Throwable e) {
                        log.error("[{}] Failed to process the request: {}", requestId, request, e);
                    }
                });
            }
        });
    }

    public void stop() {
        stopped = true;
    }

    private void reply(UUID requestId, String topic, Response response) {
        responseTemplate.send(topic, response, Collections.singletonList(new RecordHeader(TbKafkaSettings.REQUEST_ID_HEADER, uuidToBytes(requestId))));
    }

}
