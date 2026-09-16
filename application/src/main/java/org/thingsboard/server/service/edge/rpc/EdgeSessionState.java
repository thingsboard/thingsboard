// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class EdgeSessionState {

    @Setter
    private SettableFuture<Boolean> sendDownlinkMsgsFuture;
    @Setter
    private ScheduledFuture<?> scheduledSendDownlinkTask;
    @Setter
    private volatile boolean connected;
    @Setter
    private EdgeVersion edgeVersion;
    private final UUID sessionId = UUID.randomUUID();
    private final Map<Integer, DownlinkMsg> pendingMsgsMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Lock sequenceDependencyLock = new ReentrantLock();
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private TenantId tenantId;
    private Edge edge;

    public void setEdge(Edge edge) {
        if (edge == null) {
            return;
        }
        this.edge = edge;
        this.tenantId = edge.getTenantId();
    }

    public EdgeId getEdgeId() {
        return edge != null ? edge.getId() : null;
    }

    public boolean tryStartSync() {
        return syncInProgress.compareAndSet(false, true);
    }

    public void finishSync() {
        syncInProgress.set(false);
    }

    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    public void lockSequenceDependency() {
        sequenceDependencyLock.lock();
    }

    public void unlockSequenceDependency() {
        sequenceDependencyLock.unlock();
    }
}
