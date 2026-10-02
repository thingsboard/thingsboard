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

/**
 * Tool approvals awaiting a decision on this node: the channel and the turn ({@code op}, null on a single-use
 * channel) they belong to.
 */
@Component
@TbCoreComponent
public class TbAiChannelRegistry {

    public record ApprovalRoute(ChannelSession session, String op) {}

    private final ConcurrentMap<UUID, ApprovalRoute> pendingApprovals = new ConcurrentHashMap<>();

    public void registerApproval(UUID executionId, ChannelSession session, String op) {
        pendingApprovals.put(executionId, new ApprovalRoute(session, op));
    }

    public void unregisterApproval(UUID executionId) {
        pendingApprovals.remove(executionId);
    }

    public void unregisterSession(ChannelSession session) {
        pendingApprovals.values().removeIf(route -> route.session() == session);
    }

    public Optional<ApprovalRoute> findApprovalRoute(UUID executionId) {
        return Optional.ofNullable(pendingApprovals.get(executionId)).filter(route -> route.session().isOpen());
    }

}
