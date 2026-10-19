// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import org.thingsboard.server.common.data.agent.config.AgentAppConfig;

public interface HasAgentAppConfig {

    AgentAppConfig getConfig();

    void setConfig(AgentAppConfig config);
}
