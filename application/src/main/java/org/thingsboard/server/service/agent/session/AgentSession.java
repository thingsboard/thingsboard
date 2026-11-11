// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.session;

import io.grpc.Status;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface AgentSession {

    AgentSessionState getState();
    boolean push(ServerToAgent msg);
    void onError(Status status);
    void complete();
    void closeSilently();
    void drainIfPossible();
    void scheduleEventWatchdog(AgentAppEventId eventId, ScheduledExecutorService scheduler, Runnable task, long delay, TimeUnit unit);
    void cancelEventWatchdog(AgentAppEventId eventId);
}
