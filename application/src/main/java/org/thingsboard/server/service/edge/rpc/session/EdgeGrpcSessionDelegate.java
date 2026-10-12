// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.session;

import io.grpc.Status;
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

    @Override
    public void closeWithError(Status status, String errorMsg) {
        getSession().closeWithError(status, errorMsg);
    }

}
