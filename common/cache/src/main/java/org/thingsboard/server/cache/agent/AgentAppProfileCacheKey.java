// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.AgentAppProfileId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class AgentAppProfileCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AgentAppProfileId profileId;

    private AgentAppProfileCacheKey(AgentAppProfileId profileId) {
        this.profileId = profileId;
    }

    public static AgentAppProfileCacheKey forId(AgentAppProfileId profileId) {
        return new AgentAppProfileCacheKey(profileId);
    }

    @Override
    public String toString() {
        return profileId.getId().toString();
    }
}
