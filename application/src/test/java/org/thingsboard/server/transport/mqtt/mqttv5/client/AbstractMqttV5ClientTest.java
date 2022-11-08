package org.thingsboard.server.transport.mqtt.mqttv5.client;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractMqttV5ClientTest extends AbstractMqttIntegrationTest {

    protected void processClientWithPacketSizeLimitationTest() throws Exception {
        int packetSizeLimit = 99;
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setMaximumPacketSize((long) packetSizeLimit);
        options.setUserName(accessToken);

        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(options);

        MqttV5TestCallback possibleSizeCallback = updateAttributeWithStringValue(client, packetSizeLimit / 2);

        Assert.assertTrue("Server should send messages if size less then limitation.",possibleSizeCallback.getPayloadBytes().length < packetSizeLimit);

        MqttV5TestCallback bigMessageCallback = updateAttributeWithStringValue(client, packetSizeLimit * 2);

        Assert.assertNull("Server should not send a message if the message size bigger then set limit.",bigMessageCallback.getLastReceivedMessage());

        client.disconnect();

    }

    private MqttV5TestCallback updateAttributeWithStringValue(MqttV5TestClient client, int valueLen) throws Exception {
        MqttV5TestCallback onUpdateCallback = new MqttV5TestCallback();
        client.setCallback(onUpdateCallback);
        client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);

        String payload = "{\"sharedStr\":\"" + StringUtils.repeat("*", valueLen) + "\"}";

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", payload, String.class, status().isOk());
        onUpdateCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        return onUpdateCallback;
    }
}
