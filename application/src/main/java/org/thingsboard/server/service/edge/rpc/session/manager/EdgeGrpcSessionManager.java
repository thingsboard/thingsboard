// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session.manager;

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

public interface EdgeGrpcSessionManager {

    StreamObserver<RequestMsg> initInputStream(StreamObserver<ResponseMsg> outputStream,
                                               BiConsumer<EdgeId, EdgeGrpcSessionManager> sessionOpenListener,
                                               BiConsumer<Edge, UUID> sessionCloseListener,
                                               ScheduledExecutorService sendDownlinkExecutorService);
    EdgeSessionState getState();
    void addEventToHighPriorityQueue(EdgeEvent edgeEvent);
    void startSyncProcess(boolean fullSync);
    void onEdgeConnect();
    void onEdgeEventUpdate();
    void onConfigurationUpdate(Edge edge);
    void onEdgeDisconnect();
    void onEdgeRemoval();
    void destroyAndMarkAsZombieIfFailed();
    boolean destroy();

}
