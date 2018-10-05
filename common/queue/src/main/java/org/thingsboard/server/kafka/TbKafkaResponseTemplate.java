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
public class TbKafkaResponseTemplate<Request, Response> {

    private final TBKafkaConsumerTemplate<Request> requestTemplate;
    private final TBKafkaProducerTemplate<Response> responseTemplate;
    private final TbKafkaHandler<Request, Response> handler;
    private final ConcurrentMap<UUID, String> pendingRequests;
    private final ExecutorService executor;
    private final long maxPendingRequests;

    private final long pollInterval;
    private volatile boolean stopped = false;

    @Builder
    public TbKafkaResponseTemplate(TBKafkaConsumerTemplate<Request> requestTemplate,
                                   TBKafkaProducerTemplate<Response> responseTemplate,
                                   TbKafkaHandler<Request, Response> handler,
                                   long pollInterval,
                                   long maxPendingRequests,
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
            long nextCleanupMs = 0L;
            while (!stopped) {
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
                    String responseTopic = bytesToUuid(responseTopicHeader.value());
                    if (requestId == null) {
                        log.error("[{}] Missing requestId in header and body", request);
                        return;
                    }

                    Request decodedRequest = null;
                    String responseTopic = null;

                    try {
                        if (decodedRequest == null) {
                            decodedRequest = requestTemplate.decode(request);
                        }
                        executor.submit(() -> {
                            handler.handle(decodedRequest, );
                        });
                    } catch (IOException e) {
                        expectedRequest.future.setException(e);
                    }

                });
            }
        });
    }

    public void stop() {
        stopped = true;
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
        request = requestTemplate.enrich(request, responseTemplate.getTopic(), requestId);
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

    private String bytesToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
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
