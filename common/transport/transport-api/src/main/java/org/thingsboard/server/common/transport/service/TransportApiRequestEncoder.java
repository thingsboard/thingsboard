// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.queue.kafka.TbKafkaEncoder;

public class TransportApiRequestEncoder implements TbKafkaEncoder<TransportApiRequestMsg> {
    @Override
    public byte[] encode(TransportApiRequestMsg value) {
        return value.toByteArray();
    }
}
