// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToServerRpcRequestMetadata {
    private final UUID sessionId;
    private final TransportProtos.SessionType type;
    private final String nodeId;
}
