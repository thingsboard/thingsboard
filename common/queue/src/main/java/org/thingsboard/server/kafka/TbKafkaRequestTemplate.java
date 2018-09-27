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
package org.thingsboard.server.kafka;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
public class TbKafkaRequestTemplate<Request, Response> {

    private final TBKafkaProducerTemplate<Request> requestTemplate;
    private final TBKafkaConsumerTemplate<Response> responseTemplate;
    private final ConcurrentMap<UUID, ResponseMetaData<Response>> pendingRequests;
    private final boolean internalExecutor;
    private final ExecutorService executor;
    private final long maxRequestTimeout;
    private final long maxPendingRequests;
    private final long pollInterval;
    private volatile long tickTs = 0L;
    private volatile long tickSize = 0L;
    private volatile boolean stopped = false;

    @Builder
    public TbKafkaRequestTemplate(TBKafkaProducerTemplate<Request> requestTemplate, TBKafkaConsumerTemplate<Response> responseTemplate,
                                  long maxRequestTimeout,
                                  long maxPendingRequests,
                                  long pollInterval,
                                  ExecutorService executor) {
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxRequestTimeout = maxRequestTimeout;
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        if (executor != null) {
            internalExecutor = false;
            this.executor = executor;
        } else {
            internalExecutor = true;
            this.executor = Executors.newSingleThreadExecutor();
        }
    }

    public void init() {
        try {
            TBKafkaAdmin admin = new TBKafkaAdmin();
            CreateTopicsResult result = admin.createTopic(new NewTopic(responseTemplate.getTopic(), 1, (short) 1));
            result.all().get();
        } catch (Exception e) {
            log.trace("Failed to create topic: {}", e.getMessage(), e);
        }
        tickTs = System.currentTimeMillis();
        responseTemplate.subscribe();
        executor.submit(() -> {
            long nextCleanupMs = 0L;
            while (!stopped) {
                ConsumerRecords<String, byte[]> responses = responseTemplate.poll(Duration.ofMillis(pollInterval));
                responses.forEach(response -> {
                    Header requestIdHeader = response.headers().lastHeader(TbKafkaSettings.REQUEST_ID_HEADER);
                    if (requestIdHeader == null) {
                        log.error("[{}] Missing requestIdHeader", response);
                    }
                    UUID requestId = bytesToUuid(requestIdHeader.value());
                    ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
                    if (expectedResponse == null) {
                        log.trace("[{}] Invalid or stale request", requestId);
                    } else {
                        try {
                            expectedResponse.future.set(responseTemplate.decode(response));
                        } catch (IOException e) {
                            expectedResponse.future.setException(e);
                        }
                    }
                });
                tickTs = System.currentTimeMillis();
                tickSize = pendingRequests.size();
                if (nextCleanupMs < tickTs) {
                    //cleanup;
                    pendingRequests.entrySet().forEach(kv -> {
                        if (kv.getValue().expTime < tickTs) {
                            ResponseMetaData<Response> staleRequest = pendingRequests.remove(kv.getKey());
                            if (staleRequest != null) {
                                staleRequest.future.setException(new TimeoutException());
                            }
                        }
                    });
                    nextCleanupMs = tickTs + maxRequestTimeout;
                }
            }
        });
    }

    public void stop() {
        stopped = true;
        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    public ListenableFuture<Response> post(String key, Request request) {
        if (tickSize > maxPendingRequests) {
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        UUID requestId = UUID.randomUUID();
        List<Header> headers = new ArrayList<>(2);
        headers.add(new RecordHeader(TbKafkaSettings.REQUEST_ID_HEADER, uuidToBytes(requestId)));
        headers.add(new RecordHeader(TbKafkaSettings.RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic())));
        SettableFuture<Response> future = SettableFuture.create();
        pendingRequests.putIfAbsent(requestId, new ResponseMetaData<>(tickTs + maxRequestTimeout, future));
        requestTemplate.send(key, request, headers);
        return future;
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    private static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    private byte[] stringToBytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    private static class ResponseMetaData<T> {
        private final long expTime;
        private final SettableFuture<T> future;

        ResponseMetaData(long ts, SettableFuture<T> future) {
            this.expTime = ts;
            this.future = future;
        }
    }

}
