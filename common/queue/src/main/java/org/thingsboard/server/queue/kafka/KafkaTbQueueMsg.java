// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;

import java.util.UUID;

public class KafkaTbQueueMsg implements TbQueueMsg {

    private static final int UUID_LENGTH = 36;

    private final UUID key;
    private final TbQueueMsgHeaders headers;
    private final byte[] data;

    public KafkaTbQueueMsg(ConsumerRecord<String, byte[]> record) {
        if (record.key().length() <= UUID_LENGTH) {
            this.key = UUID.fromString(record.key());
        } else {
            this.key = UUID.randomUUID();
        }
        TbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        record.headers().forEach(header -> {
            headers.put(header.key(), header.value());
        });
        this.headers = headers;
        this.data = record.value();
    }

    @Override
    public UUID getKey() {
        return key;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
