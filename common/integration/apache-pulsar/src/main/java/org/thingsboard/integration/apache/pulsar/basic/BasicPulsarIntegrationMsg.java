// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.integration.apache.pulsar.PulsarIntegrationMsg;

@Data
@AllArgsConstructor
public class BasicPulsarIntegrationMsg implements PulsarIntegrationMsg {

    private final byte[] msg;
}
