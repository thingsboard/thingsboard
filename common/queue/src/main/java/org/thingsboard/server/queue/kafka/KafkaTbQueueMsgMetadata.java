// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

@Data
@AllArgsConstructor
public class KafkaTbQueueMsgMetadata implements TbQueueMsgMetadata {
    private RecordMetadata metadata;
}
