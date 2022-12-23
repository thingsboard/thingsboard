/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopic;

/**
 * Created by nickAS21 on 12.12.22
 */
@Slf4j
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler {

    private String nodeTopic;

    public SparkplugNodeSessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId, String nodeTopic) {
        super(deviceSessionCtx, sessionId);
        this.nodeTopic = nodeTopic;
    }

    public String getNodeTopic() {
        return nodeTopic;
    }

    public void onPublishMsg(ChannelHandlerContext ctx, String  topicName, int msgId, MqttPublishMessage mqttMsg) throws Exception {
        SparkplugTopic sparkplugTopic =  parseTopic(topicName);
        log.warn("SparkplugPublishMsg [{}] [{}]", sparkplugTopic.isNode() ? "node" : "device: " + sparkplugTopic.getDeviceId(), sparkplugTopic.getType());
        if (sparkplugTopic.isNode()) {
            // A node topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case NBIRTH:
                    // TODO
                    break;
                case NCMD:
                    // TODO
                    break;
                case NDATA:
                    // TODO
                    break;
                case NDEATH:
                    onGatewayNodeDisconnectProto(mqttMsg);
                    break;
                case NRECORD:
                    // TODO
                    break;
                default:
            }
        } else {
            // A device topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case DBIRTH:
                    onDeviceConnectProto(mqttMsg);
                    break;
                case DCMD:
                    // TODO
                    break;
                case DDATA:
                    // TODO
                    break;
                case DDEATH:
                    onDeviceDisconnectProto(mqttMsg);
                    break;
                case DRECORD:
                    // TODO
                    break;
                default:
            }
        }
    }

    private void onDeviceDisconnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            // TODO disconnect device without disconnect Node
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    public void handleSparkplugSubscribeMsg(List<Integer> grantedQoSList, SparkplugTopic sparkplugTopic, MqttQoS reqQoS) {
        String topicName = sparkplugTopic.toString();
        log.warn("SparkplugSubscribeMsg [{}] [{}]", sparkplugTopic.isNode() ? "node" : "device: " + sparkplugTopic.getDeviceId(), sparkplugTopic.getType());

        if (sparkplugTopic.isNode()) {
            // A node topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case NBIRTH:
                    // TODO
                    break;
                case NCMD:
                    // TODO
                    break;
                case NDATA:
                    // TODO
                    break;
                case NDEATH:
                    // TODO
                    break;
                case NRECORD:
                    // TODO
                    break;
                default:
            }
        } else {
            // A device topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case DBIRTH:
                    // TODO
                    break;
                case DCMD:
                    // TODO
                    break;
                case DDATA:
                    // TODO
                    break;
                case DDEATH:
                    // TODO
                    break;
                case DRECORD:
                    // TODO
                    break;
                default:
            }
        }
    }

}
