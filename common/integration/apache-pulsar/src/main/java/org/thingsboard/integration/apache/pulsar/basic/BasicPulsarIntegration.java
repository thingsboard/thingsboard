// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClientException;
import org.thingsboard.integration.apache.pulsar.AbstractPulsarIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicPulsarIntegration extends AbstractPulsarIntegration<BasicPulsarIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;

        loopExecutor.submit(() -> {
            while (!stopped) {
                try {
                    Messages<byte[]> messages = pulsarConsumer.batchReceive();
                    messages.forEach(msg -> process(new BasicPulsarIntegrationMsg(msg.getData())));
                    pulsarConsumer.acknowledge(messages);
                } catch (PulsarClientException e) {
                    if (!stopped) {
                        log.warn("[{}] Failed to receive messages from Apache Pulsar integration.", this.configuration.getId(), e);
                    }
                }
            }
        });
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicPulsarIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getMsg(), new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }

}
