// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.session;

import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@RequiredArgsConstructor
public class AgentSessionState {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicReference<Status> errorStatus = new AtomicReference<>();
    private TenantId tenantId;
    private AgentId agentId;

    private Agent agent;

    public boolean beginClosing() {
        return closing.compareAndSet(false, true);
    }

    public void setAgent(Agent agent) {
        this.tenantId = agent.getTenantId();
        this.agentId = agent.getId();
        this.agent = agent;
    }

    public void closeAndDo(Runnable runnable) {
        if (closed.compareAndSet(false, true)) {
            runnable.run();
        }
    }

    public void setErrorStatus(Status errorStatus) {
        this.errorStatus.compareAndSet(null, errorStatus);
    }

    public boolean isError() {
        return getErrorStatus() != null;
    }

    public Status getErrorStatus() {
        return this.errorStatus.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean isClosing() {
        return closing.get();
    }
}
