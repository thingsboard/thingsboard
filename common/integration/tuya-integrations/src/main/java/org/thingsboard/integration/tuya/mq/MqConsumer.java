// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;

import java.util.concurrent.TimeUnit;

@Builder
@Slf4j
public class MqConsumer {

    private final String serviceUrl;
    private final String accessId;
    private final String accessKey;
    private final MqEnv env;

    private final IMessageListener messageListener;
    private final ResultHandler resultHandler;

    private Consumer consumer;
    private PulsarClient client;

    private volatile boolean stopped;
    private volatile boolean connected;

    public void initClient() throws PulsarClientException {
        stopClient();
        client = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .allowTlsInsecureConnection(true)
                .authentication(new MqAuthentication(accessId, accessKey))
                .startingBackoffInterval(60, TimeUnit.SECONDS)
                .maxBackoffInterval(10, TimeUnit.MINUTES)
                .build();
    }

    public void connect() throws PulsarClientException {
        String topic = String.format("%s/out/%s", accessId, (env != null ? env : MqEnv.PROD).getValue());
        if (client == null) {
            initClient();
        }
        if (consumer == null) {
            try {
                consumer = client.newConsumer()
                        .topic(topic)
                        .subscriptionName(String.format("%s-sub", accessId))
                        .subscriptionType(SubscriptionType.Failover)
                        .subscriptionTopicsMode(RegexSubscriptionMode.AllTopics)
                        .autoUpdatePartitions(Boolean.FALSE)
                        .messageListener(((consumer1, msg) -> {
                            try {
                                messageListener.onMessageArrived(msg);
                                consumer1.acknowledge(msg);
                            } catch (Exception e) {
                                resultHandler.onResult("Uplink", "", e);
                            }
                        }))
                        .subscribe();
            } catch (PulsarClientException wrappedException) {
                if (wrappedException.getCause() == null || wrappedException.getCause().getCause() == null) {
                    throw wrappedException;
                }
                throw (PulsarClientException) wrappedException.getCause().getCause();
            }
        }
        if (!checkConnection()) {
            throw new RuntimeException("Cannot connect to message producer.");
        }
        connected = true;
    }

    public interface IMessageListener {
        void onMessageArrived(Message message);
    }

    public interface ResultHandler {
        void onResult(String type, String msg, Exception exception);
    }

    public void stopClient() throws PulsarClientException {
        stopped = true;
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    public boolean checkConnection() {
        if (client == null || client.isClosed() || consumer == null) {
            return false;
        }
        long connectionTimeoutTs = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < connectionTimeoutTs) {
            if (consumer.isConnected()) {
                return true;
            }
        }
        return false;
    }
}
