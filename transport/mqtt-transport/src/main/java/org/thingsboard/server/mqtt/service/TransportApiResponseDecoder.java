package org.thingsboard.server.mqtt.service;

import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.kafka.TbKafkaDecoder;

import java.io.IOException;

/**
 * Created by ashvayka on 05.10.18.
 */
public class TransportApiResponseDecoder implements TbKafkaDecoder<TransportApiResponseMsg> {
    @Override
    public TransportApiResponseMsg decode(byte[] data) throws IOException {
        return TransportApiResponseMsg.parseFrom(data);
    }
}
