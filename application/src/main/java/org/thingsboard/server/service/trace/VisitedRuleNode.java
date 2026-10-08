package org.thingsboard.server.service.trace;

import java.util.UUID;

public record VisitedRuleNode(UUID queueId, UUID ruleChainId, UUID ruleNodeId) {
}
