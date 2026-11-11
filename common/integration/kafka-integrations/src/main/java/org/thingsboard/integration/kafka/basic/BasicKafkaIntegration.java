// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.kafka.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.InterruptException;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.kafka.AbstractKafkaIntegration;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicKafkaIntegration extends AbstractKafkaIntegration<BasicKafkaIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);

        initConsumer(kafkaConsumerConfiguration);

        loopExecutor.submit(() -> {
            while (!stopped) {
                kafkaLock.lock();
                try {
                    ConsumerRecords<String, String> requests = kafkaConsumer.poll(Duration.ofMillis(pollInterval));
                    requests.forEach(request -> process(new BasicKafkaIntegrationMsg(request.value())));
                } catch (InterruptException ie) {
                    if (!stopped) {
                        log.warn("[{}] Fetching data from kafka was interrupted.", this.configuration.getId(), ie);
                    }
                } catch (Throwable e) {
                    log.warn("[{}] Failed to obtain messages from queue.", this.configuration.getId(), e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                } finally {
                    kafkaLock.unlock();
                }
            }
        });
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicKafkaIntegrationMsg msg) throws Exception {
        byte[] bytes = msg.getMsg().getBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, bytes, new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }

}
