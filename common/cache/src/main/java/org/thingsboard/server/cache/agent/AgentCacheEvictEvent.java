// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.AgentId;

@Data
@RequiredArgsConstructor
public class AgentCacheEvictEvent {

    private final AgentId agentId;
    private final String routingKey;
    private final String oldRoutingKey;

}
