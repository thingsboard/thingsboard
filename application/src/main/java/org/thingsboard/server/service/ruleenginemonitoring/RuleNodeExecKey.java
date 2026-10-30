// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import java.util.UUID;

public record RuleNodeExecKey(
        UUID tenantId,
        UUID queueId,
        String queueName,
        UUID ruleChainId,
        UUID ruleNodeId,
        String serviceId
) {}
