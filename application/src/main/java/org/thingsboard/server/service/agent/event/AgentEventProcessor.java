// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;

public interface AgentEventProcessor {

    void onEventNotification(AgentAppEventNotificationProto notification);

    void resumeEventsOnReconnect(TenantId tenantId, AgentId agentId);

    void processNextEventForApp(TenantId tenantId, AgentId agentId, AgentApplication application);

    void processNextStepOrFinish(TenantId tenantId, AgentId agentId, AgentAppEvent event);

    void onError(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, String errorMsg);

    void dispatchAllQueuedAppEvents(TenantId tenantId, AgentId agentId);
}
