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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbMsgQueueProducer;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgPublishParams;

import java.util.Properties;

@Slf4j
public class TbKafkaMsgQueueProducer implements TbMsgQueueProducer {

    private final KafkaProducer<String, byte[]> producer;

    @Builder
    private TbKafkaMsgQueueProducer(TbProducerSettings settings, String clientId) {
        Properties props = settings.toProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void publish(TbMsgPublishParams publishParams, TbMsg msg, FutureCallback<Void> callback) {
        byte[] data = TbMsg.toByteArray(msg);
        producer.send(new ProducerRecord<>(publishParams.getTopic(), msg.getOriginator().toString(), data), new ProducerCallbackAdaptor(callback));
    }

    private static class ProducerCallbackAdaptor implements Callback {
        private final FutureCallback<Void> callback;

        ProducerCallbackAdaptor(FutureCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception == null) {
                log.debug("[{} {}] Sent record to kafka!", metadata.topic(), metadata.offset());
                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                log.error("[{}] Failed to send record to kafka - offset {}!", metadata.topic(), metadata.offset(), exception);
                if (callback != null) {
                    callback.onFailure(exception);
                }
            }
        }
    }

}
