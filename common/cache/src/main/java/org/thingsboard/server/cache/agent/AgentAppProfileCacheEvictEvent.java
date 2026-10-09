// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.AgentAppProfileId;

@Data
@RequiredArgsConstructor
public class AgentAppProfileCacheEvictEvent {

    private final AgentAppProfileId profileId;

}
