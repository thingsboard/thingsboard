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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.common.stats.MessagesStats;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DefaultTbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueRequestTemplate<Request, Response> {

    private final TbQueueAdmin queueAdmin;
    private final TbQueueProducer<Request> requestTemplate;
    private final TbQueueConsumer<Response> responseTemplate;
    final ConcurrentMap<UUID, DefaultTbQueueRequestTemplate.ResponseMetaData<Response>> pendingRequests;
    final boolean internalExecutor;
    final ExecutorService executor;
    final long maxRequestTimeout;
    final long maxPendingRequests;
    final long pollInterval;
    volatile long tickTs = 0L;
    volatile long tickSize = 0L;
    volatile boolean stopped = false;
    long nextCleanupMs = 0L;

    private MessagesStats messagesStats;

    @Builder
    public DefaultTbQueueRequestTemplate(TbQueueAdmin queueAdmin,
                                         TbQueueProducer<Request> requestTemplate,
                                         TbQueueConsumer<Response> responseTemplate,
                                         long maxRequestTimeout,
                                         long maxPendingRequests,
                                         long pollInterval,
                                         @Nullable ExecutorService executor) {
        this.queueAdmin = queueAdmin;
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxRequestTimeout = maxRequestTimeout;
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.internalExecutor = (executor == null);
        this.executor = internalExecutor ? createExecutor() : executor;
    }

    ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-queue-request-template-" + responseTemplate.getTopic()));
    }

    @Override
    public void init() {
        queueAdmin.createTopicIfNotExists(responseTemplate.getTopic());
        requestTemplate.init();
        tickTs = getCurrentTime();
        responseTemplate.subscribe();
        executor.submit(this::mainLoop);
    }

    void mainLoop() {
        while (!stopped) {
            try {
                fetchAndProcessResponses();
            } catch (Throwable e) {
                log.warn("Failed to obtain responses from queue. Going to sleep " + pollInterval + "ms", e);
                sleep();
            }
        }
    }

    void fetchAndProcessResponses() {
        final int pendingRequestsCount = pendingRequests.size();
        log.info("Starting template pool topic {}, for pendingRequests {}", responseTemplate.getTopic(), pendingRequestsCount);
        List<Response> responses = doPoll(); //poll js responses
        //if (responses.size() > 0) {
        log.trace("Completed template poll topic {}, for pendingRequests [{}], received [{}]", responseTemplate.getTopic(), pendingRequestsCount, responses.size());
        //}
        responses.forEach(this::processResponse); //this can take a long time
        responseTemplate.commit();
        tickTs = getCurrentTime();
        tickSize = pendingRequests.size();
        if (nextCleanupMs < tickTs) {
            //cleanup;
            pendingRequests.forEach((key, value) -> {
                if (value.expTime < tickTs) {
                    ResponseMetaData<Response> staleRequest = pendingRequests.remove(key);
                    if (staleRequest != null) {
                        setTimeoutException(key, staleRequest, tickTs);
                    }
                }
            });
            setupNextCleanup();
        }
    }

    void setupNextCleanup() {
        nextCleanupMs = tickTs + maxRequestTimeout;
        log.info("setupNextCleanup {}", nextCleanupMs);
    }

    List<Response> doPoll() {
        return responseTemplate.poll(pollInterval);
    }

    void sleep() {
        try {
            Thread.sleep(pollInterval);
        } catch (InterruptedException e2) {
            log.trace("Failed to wait until the server has capacity to handle new responses", e2);
        }
    }

    void setTimeoutException(UUID key, ResponseMetaData<Response> staleRequest, long tickTs) {
        if (tickTs >= staleRequest.getSubmitTime() + staleRequest.getTimeout()) {
            log.info("Request timeout detected, tickTs [{}], {}, key [{}]", tickTs, staleRequest, key);
        } else {
            log.error("Request timeout detected, tickTs [{}], {}, key [{}]", tickTs, staleRequest, key);
        }

        staleRequest.future.setException(new TimeoutException());
    }

    void processResponse(Response response) {
        byte[] requestIdHeader = response.getHeaders().get(REQUEST_ID_HEADER);
        UUID requestId;
        if (requestIdHeader == null) {
            log.error("[{}] Missing requestId in header and body", response);
        } else {
            requestId = bytesToUuid(requestIdHeader);
            log.trace("[{}] Response received: {}", requestId, String.valueOf(response).replace("\n", " ")); //TODO remove overhead
            ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
            if (expectedResponse == null) {
                log.warn("[{}] Invalid or stale request, response: {}", requestId, String.valueOf(response).replace("\n", " "));
            } else {
                expectedResponse.future.set(response);
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;

        if (responseTemplate != null) {
            responseTemplate.unsubscribe();
        }

        if (requestTemplate != null) {
            requestTemplate.stop();
        }

        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    @Override
    public void setMessagesStats(MessagesStats messagesStats) {
        this.messagesStats = messagesStats;
    }

    @Override
    public ListenableFuture<Response> send(Request request) {
        if (tickSize > maxPendingRequests) {
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        UUID requestId = UUID.randomUUID();
        request.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        request.getHeaders().put(RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic()));
        long currentTime = getCurrentTime();
        request.getHeaders().put(REQUEST_TIME, longToBytes(currentTime));
        SettableFuture<Response> future = SettableFuture.create();
        ResponseMetaData<Response> responseMetaData = new ResponseMetaData<>(tickTs + maxRequestTimeout, future, currentTime, maxRequestTimeout);
        log.info("pending {}", responseMetaData);
        pendingRequests.putIfAbsent(requestId, responseMetaData);
        sendToRequestTemplate(request, requestId, future, responseMetaData);
        return future;
    }

    long getCurrentTime() {
        return System.currentTimeMillis();
    }

    void sendToRequestTemplate(Request request, UUID requestId, SettableFuture<Response> future, ResponseMetaData<Response> responseMetaData) {
        log.trace("[{}] Sending request, key [{}], expTime [{}], request {}", requestId, request.getKey(), responseMetaData.expTime, String.valueOf(request).replace("\n", " "));
        if (messagesStats != null) {
            messagesStats.incrementTotal();
        }
        requestTemplate.send(TopicPartitionInfo.builder().topic(requestTemplate.getDefaultTopic()).build(), request, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                if (messagesStats != null) {
                    messagesStats.incrementSuccessful();
                }
                log.trace("[{}] Request sent: {}, request {}", requestId, metadata, String.valueOf(request).replace("\n", " "));
            }

            @Override
            public void onFailure(Throwable t) {
                if (messagesStats != null) {
                    messagesStats.incrementFailed();
                }
                pendingRequests.remove(requestId);
                future.setException(t);
            }
        });
    }

    @Getter
    static class ResponseMetaData<T> {
        private final long submitTime;
        private final long timeout;
        private final long expTime;
        private final SettableFuture<T> future;

        ResponseMetaData(long ts, SettableFuture<T> future, long submitTime, long timeout) {
            this.submitTime = submitTime;
            this.timeout = timeout;
            this.expTime = ts;
            this.future = future;
        }

        @Override
        public String toString() {
            return "ResponseMetaData{" +
                    "submitTime=" + submitTime +
                    ", calculatedExpTime=" + (submitTime + timeout) +
                    ", expTime=" + expTime +
                    ", deltaMs=" + (expTime - submitTime) +
                    ", future=" + future +
                    '}';
        }
    }

}
