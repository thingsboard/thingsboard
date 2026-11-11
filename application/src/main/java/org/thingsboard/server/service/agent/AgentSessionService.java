// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import io.grpc.Status;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.service.agent.session.AgentSession;

import java.util.Optional;

public interface AgentSessionService {

    Optional<Status> onConnected(AgentSession session, Hello msg);

    void onCompleted(AgentSession session);

    void onError(AgentSession session);

}
