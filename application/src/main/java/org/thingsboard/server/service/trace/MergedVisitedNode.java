package org.thingsboard.server.service.trace;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public final class MergedVisitedNode {
    private final UUID queueId;
    private final UUID ruleChainId;
    private final UUID ruleNodeId;
    private final Set<UUID> incoming = new HashSet<>();
    private final Set<UUID> outgoing = new HashSet<>();

    MergedVisitedNode(UUID queueId, UUID ruleChainId, UUID ruleNodeId) {
        this.queueId = queueId;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
    }
}
