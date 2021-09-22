package org.thingsboard.server.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.AbstractTransportObserver;
import org.thingsboard.server.OnPingCallback;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import javax.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.util.UUID;

@Component
@Slf4j
public class MqttObserver extends AbstractTransportObserver {

    private WebSocketClientImpl webSocketClient;

    @Value("${mqtt.monitoring_rate}")
    private int monitoringRate;
    
    @Value("${mqtt.url}")
    private String mqttUrl;

    @Value("${mqtt.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.timeout}")
    private long timeout;

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    @Value("${mqtt.test_device.id}")
    private UUID testDeviceUuid;

    private MqttAsyncClient mqttAsyncClient;

    public MqttObserver() {
        super();
    }

    @PostConstruct
    private void init() {
        try {
            mqttAsyncClient = getMqttAsyncClient();

            webSocketClient = buildAndConnectWebSocketClient();
            webSocketClient.send(mapper.writeValueAsString(getTelemetryCmdsWrapper(testDeviceUuid)));
            webSocketClient.waitForReply(websocketWaitTime);
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    @Override
    public String pingTransport(String payload) throws Exception {
        webSocketClient.registerWaitForUpdate();
        publishMqttMsg(mqttAsyncClient, payload.getBytes(), DEVICE_TELEMETRY_TOPIC);
        return webSocketClient.waitForUpdate(websocketWaitTime);
    }
    
    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    private MqttAsyncClient getMqttAsyncClient() throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(mqttUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(testDeviceAccessToken);
        client.connect(options).waitForCompletion(timeout);
        return client;
    }

    private void publishMqttMsg(MqttAsyncClient client, byte[] payload, String topic) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        message.setQos(qos);
        client.publish(topic, message);
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
    }
    
    
    
    
}
