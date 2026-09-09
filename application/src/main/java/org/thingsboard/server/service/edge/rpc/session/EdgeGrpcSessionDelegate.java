// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

public abstract class EdgeGrpcSessionDelegate implements EdgeGrpcSessionManager {

    protected abstract EdgeSession getSession();

    @Override
    public void addEventToHighPriorityQueue(EdgeEvent edgeEvent) {
        getSession().addHighPriorityEvent(edgeEvent);
    }

    @Override
    public void startSyncProcess(boolean fullSync) {
        getSession().startSyncProcess(fullSync);
    }
}
