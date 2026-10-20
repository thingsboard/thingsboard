// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent.util;

import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.util.ArrayList;

public final class AgentProtoUtils {

    private AgentProtoUtils() {}

    public static LogChunk fromProto(org.thingsboard.server.gen.agent.v1.AgentLogChunk proto) {
        return new LogChunk(
                proto.getUnitId(),
                new ArrayList<>(proto.getLinesList()),
                proto.getDropped(),
                proto.getLastLineTs());
    }

}
