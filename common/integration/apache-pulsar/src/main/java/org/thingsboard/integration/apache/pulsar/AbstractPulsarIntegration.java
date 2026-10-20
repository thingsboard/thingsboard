// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.integration.api.util.ConvertUtil.toDebugMessage;

@Slf4j
public abstract class AbstractPulsarIntegration<T extends PulsarIntegrationMsg> extends AbstractIntegration<T> {

    protected PulsarConfiguration pulsarConfiguration;
    protected IntegrationContext ctx;
    protected ExecutorService loopExecutor;
    protected volatile boolean stopped = false;
    protected PulsarClient pulsarClient;
    protected Consumer<byte[]> pulsarConsumer;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        this.ctx = params.getContext();
        pulsarConfiguration = getClientConfiguration(configuration, PulsarConfiguration.class);

        pulsarClient = PulsarClient.builder()
                .authentication(pulsarConfiguration.getCredentials().getAuthentication())
                .serviceUrl(pulsarConfiguration.getServiceUrl())
                .allowTlsInsecureConnection(true)
                .startingBackoffInterval(60, TimeUnit.SECONDS)
                .maxBackoffInterval(10, TimeUnit.MINUTES)
                .build();

        BatchReceivePolicy batchReceivePolicy = BatchReceivePolicy.builder()
                .maxNumMessages(pulsarConfiguration.getMaxNumMessages())
                .maxNumBytes(pulsarConfiguration.getMaxNumBytes())
                .timeout(pulsarConfiguration.getTimeoutInMs(), TimeUnit.MILLISECONDS)
                .build();

        pulsarConsumer = pulsarClient
                .newConsumer()
                .subscriptionName(pulsarConfiguration.getSubscriptionName())
                .topic(pulsarConfiguration.getTopics().split(","))
                .batchReceivePolicy(batchReceivePolicy)
                .subscribe();
    }

    @Override
    public void process(T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
        }
        persistDebug(context, "Uplink", ContentType.BINARY, () -> toDebugMessage(ContentType.BINARY, msg.getMsg()), status, exception);
    }

    @Override
    public void destroy() {
        stopped = true;
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }

        if (pulsarConsumer != null) {
            try {
                pulsarConsumer.close();
            } catch (PulsarClientException e) {
                log.warn("Failed to stop Apache Pulsar Consumer!!!", e);
            }
        }

        if (pulsarClient != null) {
            try {
                pulsarClient.close();
            } catch (PulsarClientException e) {
                log.warn("Failed to stop Apache Pulsar Client!!!", e);
            }
        }
    }

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

}
