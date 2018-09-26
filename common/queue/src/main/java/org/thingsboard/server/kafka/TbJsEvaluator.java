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
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbJsEvaluator {

//    public static void main(String[] args) {
//        ExecutorService executorService = Executors.newCachedThreadPool();
//
//        TBKafkaConsumerTemplate requestConsumer = new TBKafkaConsumerTemplate();
//        requestConsumer.subscribe("requests");
//
//        LongAdder responseCounter = new LongAdder();
//        TBKafkaProducerTemplate responseProducer = new TBKafkaProducerTemplate();
//        executorService.submit((Runnable) () -> {
//            while (true) {
//                ConsumerRecords<String, String> requests = requestConsumer.poll(100);
//                requests.forEach(request -> {
//                    Header header = request.headers().lastHeader("responseTopic");
//                    ProducerRecord<String, String> response = new ProducerRecord<>(new String(header.value(), StandardCharsets.UTF_8),
//                            request.key(), request.value());
//                    responseProducer.send(response);
//                    responseCounter.add(1);
//                });
//            }
//        });
//
//        executorService.submit((Runnable) () -> {
//            while (true) {
//                log.warn("Requests: [{}], Responses: [{}]", responseCounter.longValue(), responseCounter.longValue());
//                try {
//                    Thread.sleep(1000L);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//    }

}
