// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@TbCoreComponent
public class TbAiChannelRegistry {

    private final ConcurrentMap<UUID, ChannelSession> pendingApprovals = new ConcurrentHashMap<>();

    public void registerApproval(UUID executionId, ChannelSession session) {
        pendingApprovals.put(executionId, session);
    }

    public void unregisterApproval(UUID executionId) {
        pendingApprovals.remove(executionId);
    }

    public void unregisterSession(ChannelSession session) {
        pendingApprovals.values().removeIf(registered -> registered == session);
    }

    public Optional<ChannelSession> findApprovalSession(UUID executionId) {
        return Optional.ofNullable(pendingApprovals.get(executionId)).filter(ChannelSession::isOpen);
    }

}
