// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import java.util.UUID;

public record QueueRowKey(UUID queueTenantId, UUID tenantId, UUID queueId,
                          UUID ruleChainId, UUID lastVisitedRuleNodeId, String serviceId) {}
