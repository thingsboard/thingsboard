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

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbRuleEngineEmulator {
//
//    public static void main(String[] args) throws InterruptedException, ExecutionException {
//        ConcurrentMap<String, String> pendingRequestsMap = new ConcurrentHashMap<>();
//
//        ExecutorService executorService = Executors.newCachedThreadPool();
//
//        String responseTopic = "server" + Math.abs((int) (5000.0 * Math.random()));
//        try {
//            TBKafkaAdmin admin = new TBKafkaAdmin();
//            CreateTopicsResult result = admin.createTopic(new NewTopic(responseTopic, 1, (short) 1));
//            result.all().get();
//        } catch (Exception e) {
//            log.warn("Failed to create topic: {}", e.getMessage(), e);
//        }
//
//        List<Header> headers = Collections.singletonList(new RecordHeader("responseTopic", responseTopic.getBytes(StandardCharsets.UTF_8)));
//
//        TBKafkaConsumerTemplate responseConsumer = new TBKafkaConsumerTemplate();
//        TBKafkaProducerTemplate requestProducer = new TBKafkaProducerTemplate();
//
//        LongAdder requestCounter = new LongAdder();
//        LongAdder responseCounter = new LongAdder();
//
//        responseConsumer.subscribe(responseTopic);
//        executorService.submit((Runnable) () -> {
//            while (true) {
//                ConsumerRecords<String, String> responses = responseConsumer.poll(100);
//                responses.forEach(response -> {
//                    String expectedResponse = pendingRequestsMap.remove(response.key());
//                    if (expectedResponse == null) {
//                        log.error("[{}] Invalid request", response.key());
//                    } else if (!expectedResponse.equals(response.value())) {
//                        log.error("[{}] Invalid response: {} instead of {}", response.key(), response.value(), expectedResponse);
//                    }
//                    responseCounter.add(1);
//                });
//            }
//        });
//
//        executorService.submit((Runnable) () -> {
//            int i = 0;
//            while (true) {
//                String requestId = UUID.randomUUID().toString();
//                String expectedResponse = UUID.randomUUID().toString();
//                pendingRequestsMap.put(requestId, expectedResponse);
//                requestProducer.send(new ProducerRecord<>("requests", null, requestId, expectedResponse, headers));
//                requestCounter.add(1);
//                i++;
//                if (i % 10000 == 0) {
//                    try {
//                        Thread.sleep(500L);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        executorService.submit((Runnable) () -> {
//            while (true) {
//                log.warn("Requests: [{}], Responses: [{}]", requestCounter.longValue(), responseCounter.longValue());
//                try {
//                    Thread.sleep(1000L);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        Thread.sleep(60000);
//    }

}
