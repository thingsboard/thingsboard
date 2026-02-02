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
package org.thingsboard.server.queue.common;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class PartitionedQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate {

    @Getter
    private final PartitionedQueueConsumerManager<Request> requestConsumer;
    private final TbQueueProducer<Response> responseProducer;

    private final TbQueueHandler<Request, Response> handler;
    private final long pollInterval;
    private final int maxPendingRequests;
    private final long requestTimeout;
    private final MessagesStats stats;

    private final ScheduledExecutorService scheduler;
    private final ExecutorService callbackExecutor;

    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    @Builder
    public PartitionedQueueResponseTemplate(String key,
                                            TbQueueHandler<Request, Response> handler,
                                            String requestsTopic,
                                            Function<TopicPartitionInfo, TbQueueConsumer<Request>> consumerCreator,
                                            TbQueueProducer<Response> responseProducer,
                                            long pollInterval,
                                            long requestTimeout,
                                            int maxPendingRequests,
                                            ExecutorService consumerExecutor,
                                            ExecutorService callbackExecutor,
                                            ExecutorService consumerTaskExecutor,
                                            MessagesStats stats) {
        this.scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor(key + "-queue-response-template-scheduler");
        this.callbackExecutor = callbackExecutor;
        this.handler = handler;
        this.requestConsumer = PartitionedQueueConsumerManager.<Request>create()
                .queueKey(key + "-requests")
                .topic(requestsTopic)
                .pollInterval(pollInterval)
                .msgPackProcessor((requests, consumer, consumerKey, config) -> processRequests(requests, consumer))
                .consumerCreator((config, tpi) -> consumerCreator.apply(tpi))
                .consumerExecutor(consumerExecutor)
                .scheduler(scheduler)
                .taskExecutor(consumerTaskExecutor)
                .build();
        this.responseProducer = responseProducer;
        this.pollInterval = pollInterval;
        this.maxPendingRequests = maxPendingRequests;
        this.requestTimeout = requestTimeout;
        this.stats = stats;
    }

    private void processRequests(List<Request> requests, TbQueueConsumer<Request> consumer) {
        while (pendingRequestCount.get() >= maxPendingRequests) {
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.trace("Failed to wait until the server has capacity to handle new requests", e);
            }
        }

        requests.forEach(request -> {
            long currentTime = System.currentTimeMillis();
            long expireTs = bytesToLong(request.getHeaders().get(EXPIRE_TS_HEADER));
            if (expireTs >= currentTime) {
                byte[] requestIdHeader = request.getHeaders().get(REQUEST_ID_HEADER);
                if (requestIdHeader == null) {
                    log.error("[{}] Missing requestId in header", request);
                    return;
                }
                byte[] responseTopicHeader = request.getHeaders().get(RESPONSE_TOPIC_HEADER);
                if (responseTopicHeader == null) {
                    log.error("[{}] Missing response topic in header", request);
                    return;
                }
                UUID requestId = bytesToUuid(requestIdHeader);
                String responseTopic = bytesToString(responseTopicHeader);
                try {
                    pendingRequestCount.getAndIncrement();
                    stats.incrementTotal();
                    AsyncCallbackTemplate.withCallbackAndTimeout(handler.handle(request),
                            response -> {
                                pendingRequestCount.decrementAndGet();
                                response.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
                                TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(responseTopic).build();
                                responseProducer.send(tpi, response, new TbQueueCallback() {
                                    @Override
                                    public void onSuccess(TbQueueMsgMetadata metadata) {
                                        stats.incrementSuccessful();
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        log.error("[{}] Failed to send response {}", requestId, response, t);
                                        sendErrorResponse(requestId, tpi, request, t);
                                        stats.incrementFailed();
                                    }
                                });
                            },
                            e -> {
                                pendingRequestCount.decrementAndGet();
                                if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                                    log.warn("[{}] Timeout to process the request: {}", requestId, request, e);
                                } else {
                                    log.trace("[{}] Failed to process the request: {}", requestId, request, e);
                                }
                                stats.incrementFailed();
                            },
                            requestTimeout,
                            scheduler,
                            callbackExecutor);
                } catch (Throwable e) {
                    pendingRequestCount.decrementAndGet();
                    log.warn("[{}] Failed to process the request: {}", requestId, request, e);
                    stats.incrementFailed();
                }
            }
        });
        consumer.commit();
    }

    private void sendErrorResponse(UUID requestId, TopicPartitionInfo tpi, Request request, Throwable cause) {
        Response errorResponseMsg = handler.constructErrorResponseMsg(request, cause);

        if (errorResponseMsg != null) {
            errorResponseMsg.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
            responseProducer.send(tpi, errorResponseMsg, null);
        }
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        requestConsumer.update(partitions);
    }

    public void stop() {
        if (requestConsumer != null) {
            requestConsumer.stop();
            requestConsumer.awaitStop();
        }
        if (responseProducer != null) {
            responseProducer.stop();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
