// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToServerRpcRequestMetadata {
    private final UUID sessionId;
    private final SessionType type;
    private final String nodeId;
}
