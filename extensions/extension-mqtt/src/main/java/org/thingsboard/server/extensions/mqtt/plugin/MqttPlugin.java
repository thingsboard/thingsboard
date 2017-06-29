/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.mqtt.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.mqtt.action.MqttPluginAction;

import java.util.UUID;

@Plugin(name = "Mqtt Plugin", actions = {MqttPluginAction.class},
        descriptor = "MqttPluginDescriptor.json", configuration = MqttPluginConfiguration.class)
@Slf4j
public class MqttPlugin extends AbstractPlugin<MqttPluginConfiguration> {

    private MqttMsgHandler handler;

    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttClientOptions;

    private int retryInterval;

    private final Object connectLock = new Object();

    @Override
    public void init(MqttPluginConfiguration configuration) {
        retryInterval = configuration.getRetryInterval();

        mqttClientOptions = new MqttConnectOptions();
        mqttClientOptions.setCleanSession(false);
        mqttClientOptions.setMaxInflight(configuration.getMaxInFlight());
        mqttClientOptions.setAutomaticReconnect(true);
        String clientId = configuration.getClientId();
        if (StringUtils.isEmpty(clientId)) {
            clientId = UUID.randomUUID().toString();
        }
        if (!StringUtils.isEmpty(configuration.getAccessToken())) {
            mqttClientOptions.setUserName(configuration.getAccessToken());
        }
        try {
            mqttClient = new MqttAsyncClient("tcp://" + configuration.getHost() + ":" + configuration.getPort(), clientId);
        } catch (Exception e) {
            log.error("Failed to create mqtt client", e);
            throw new RuntimeException(e);
        }
        connect();
    }

    private void connect() {
        if (!mqttClient.isConnected()) {
            synchronized (connectLock) {
                while (!mqttClient.isConnected()) {
                    log.debug("Attempt to connect to requested mqtt host [{}]!", mqttClient.getServerURI());
                    try {
                        mqttClient.connect(mqttClientOptions, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                log.info("Connected to requested mqtt host [{}]!", mqttClient.getServerURI());
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable e) {
                            }
                        }).waitForCompletion();
                    } catch (MqttException e) {
                        log.warn("Failed to connect to requested mqtt host  [{}]!", mqttClient.getServerURI(), e);
                        if (!mqttClient.isConnected()) {
                            try {
                                Thread.sleep(retryInterval);
                            } catch (InterruptedException e1) {
                                log.trace("Failed to wait for retry interval!", e);
                            }
                        }
                    }
                }
            }
        }
        this.handler = new MqttMsgHandler(mqttClient);
    }

    private void destroy() {
        try {
            this.handler = null;
            this.mqttClient.disconnect();
        } catch (MqttException e) {
            log.error("Failed to close mqtt client connection during destroy()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        connect();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
