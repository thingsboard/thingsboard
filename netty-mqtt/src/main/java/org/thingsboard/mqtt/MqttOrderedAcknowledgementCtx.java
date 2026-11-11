// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.mqtt;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public class MqttOrderedAcknowledgementCtx {

    private final String clientId;
    private final MqttVersion mqttVersion;
    private final MqttMessageType mqttMessageType;

    private final Queue<MqttMsgWrapper> receivedMsgQueue = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock();

    public MqttMsgWrapper addMsgId(int msgId) {
        if (msgId == -1) {
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("[{}] Adding received msgId: {}", clientId, msgId);
        }
        MqttMsgWrapper msgStatus = MqttMsgWrapper.builder().id(msgId).build();
        receivedMsgQueue.add(msgStatus);
        return msgStatus;
    }

    public void ack(Channel channel, MqttMsgWrapper msgWrapper, byte reasonCodeValue, boolean sendAck) {
        if (msgWrapper == null) {
            return;
        }
        msgWrapper.setReasonCode(reasonCodeValue);
        msgWrapper.setAck(true);
        log.trace("[{}] Processing ack msg {} with reasonCode {}", clientId, msgWrapper, reasonCodeValue);

        tryProcess(channel, sendAck);
    }

    private void tryProcess(Channel channel, boolean sendAck) {
        MqttMsgWrapper head;
        while ((head = receivedMsgQueue.peek()) != null && head.isAck()) {
            //try to lock to poll queue exclusively
            if (!lock.tryLock()) {
                return;
            }
            try {
                while ((head = receivedMsgQueue.peek()) != null && head.isAck()) {
                    final MqttMsgWrapper polled = receivedMsgQueue.poll();
                    if (head != polled) {  // double check
                        throw new RuntimeException("polled head [" + polled + "] does not the same as peeked head [" + head + "]. Msg order broken. Queue left behind: " + receivedMsgQueue);
                    }
                    if (sendAck) {
                        sendAckMsg(channel, polled.getId(), polled.getReasonCode());
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void sendAckMsg(Channel channel, int msgId, byte reasonCodeValue) {
        log.trace("[{}] Sending {} for msgId {} with reasonCode {}", clientId, mqttMessageType, msgId, reasonCodeValue);
        MqttMessage message = switch (mqttMessageType) {
            case PUBACK -> buildPubAck(msgId, reasonCodeValue);
            case PUBREC -> buildPubRec(msgId, reasonCodeValue);
            default -> throw new IllegalStateException("Unexpected value: " + mqttMessageType);
        };
        channel.writeAndFlush(message);
    }

    private MqttMessage buildPubAck(int msgId, byte reasonCodeValue) {
        var builder = MqttMessageBuilders.pubAck().packetId(msgId);
        if (MqttVersion.MQTT_5.equals(mqttVersion)) {
            builder.reasonCode(reasonCodeValue);
        }
        return builder.build();
    }

    protected MqttMessage buildPubRec(int msgId, byte reasonCodeValue) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(mqttMessageType, false, MqttQoS.AT_MOST_ONCE, false, 0);
        if (MqttVersion.MQTT_5.equals(mqttVersion)) {
            return new MqttMessage(mqttFixedHeader, new MqttPubReplyMessageVariableHeader(msgId, reasonCodeValue, null));
        }
        return new MqttMessage(mqttFixedHeader, MqttMessageIdVariableHeader.from(msgId));
    }

    @Builder
    @Getter
    @ToString // The @Data (equals and hashcode) is not suitable here. The usage does not expect that we will search the object somehow. Only having a link to the object
    public static class MqttMsgWrapper {
        private final int id;
        @Setter
        private volatile byte reasonCode;
        @Setter
        private volatile boolean ack;
    }

}
