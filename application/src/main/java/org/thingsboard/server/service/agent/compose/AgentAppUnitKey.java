// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import org.thingsboard.server.common.data.agent.AgentAppUnitType;

/**
 * Identity of an agent app unit within an application: compose namespaces services, volumes and
 * networks separately, so the same name can map to distinct units of different types.
 */
public record AgentAppUnitKey(AgentAppUnitType type, String identifier) {
}
