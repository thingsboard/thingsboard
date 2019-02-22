/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
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
    private final ExecutorService loopExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final ExecutorService callbackExecutor;
    private final int maxPendingRequests;
    private final long requestTimeout;

    private final long pollInterval;
    private volatile boolean stopped = false;
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    @Builder
    public TbKafkaResponseTemplate(TBKafkaConsumerTemplate<Request> requestTemplate,
                                   TBKafkaProducerTemplate<Response> responseTemplate,
                                   TbKafkaHandler<Request, Response> handler,
                                   long pollInterval,
                                   long requestTimeout,
                                   int maxPendingRequests,
                                   ExecutorService executor) {
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.handler = handler;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.requestTimeout = requestTimeout;
        this.callbackExecutor = executor;
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        this.loopExecutor = Executors.newSingleThreadExecutor();
    }

    public void init() {
        this.responseTemplate.init();
        requestTemplate.subscribe();
        loopExecutor.submit(() -> {
            while (!stopped) {
                try {
                    while (pendingRequestCount.get() >= maxPendingRequests) {
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException e) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e);
                        }
                    }
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
                            pendingRequestCount.getAndIncrement();
                            Request decodedRequest = requestTemplate.decode(request);
                            AsyncCallbackTemplate.withCallbackAndTimeout(handler.handle(decodedRequest),
                                    response -> {
                                        pendingRequestCount.decrementAndGet();
                                        reply(requestId, responseTopic, response);
                                    },
                                    e -> {
                                        pendingRequestCount.decrementAndGet();
                                        if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                                            log.warn("[{}] Timedout to process the request: {}", requestId, request, e);
                                        } else {
                                            log.trace("[{}] Failed to process the request: {}", requestId, request, e);
                                        }
                                    },
                                    requestTimeout,
                                    timeoutExecutor,
                                    callbackExecutor);
                        } catch (Throwable e) {
                            pendingRequestCount.decrementAndGet();
                            log.warn("[{}] Failed to process the request: {}", requestId, request, e);
                        }
                    });
                } catch (InterruptException ie) {
                    if (!stopped) {
                        log.warn("Fetching data from kafka was interrupted.", ie);
                    }
                } catch (Throwable e) {
                    log.warn("Failed to obtain messages from queue.", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        });
    }

    public void stop() {
        stopped = true;
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }
    }

    private void reply(UUID requestId, String topic, Response response) {
        responseTemplate.send(topic, requestId.toString(), response, Collections.singletonList(new RecordHeader(TbKafkaSettings.REQUEST_ID_HEADER, uuidToBytes(requestId))), null);
    }

}
