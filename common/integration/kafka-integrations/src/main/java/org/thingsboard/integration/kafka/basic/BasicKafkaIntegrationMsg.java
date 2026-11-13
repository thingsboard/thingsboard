// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.kafka.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.integration.kafka.KafkaIntegrationMsg;

@Data
@AllArgsConstructor
public class BasicKafkaIntegrationMsg implements KafkaIntegrationMsg {

    private final String msg;
}
