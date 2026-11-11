// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;

import java.util.concurrent.ScheduledExecutorService;

public interface AgentEventWatchdog {

    void schedule(AgentApplication application, AgentAppEvent event, AgentEventResender resender);

    void cancel(AgentId agentId, AgentAppEventId eventId);

    ScheduledExecutorService getScheduler();

}
