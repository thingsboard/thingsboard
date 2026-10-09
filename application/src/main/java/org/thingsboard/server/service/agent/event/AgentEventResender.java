// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;

public interface AgentEventResender {

    void resendCurrentStep(AgentAppEvent event, AgentApplication application, AgentAppStep step);

    void onError(AgentAppEventId eventId, AgentApplication application);
}
