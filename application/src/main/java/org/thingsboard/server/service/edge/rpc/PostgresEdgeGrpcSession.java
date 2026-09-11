// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

@Slf4j
public class PostgresEdgeGrpcSession extends EdgeGrpcSession {

    PostgresEdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream,
                            BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                            BiConsumer<Edge, UUID> sessionCloseListener, ScheduledExecutorService sendDownlinkExecutorService,
                            int maxInboundMessageSize, int maxHighPriorityQueueSizePerSession) {
        super(ctx, outputStream, sessionOpenListener, sessionCloseListener, sendDownlinkExecutorService, maxInboundMessageSize, maxHighPriorityQueueSizePerSession);
    }

    @Override
    public ListenableFuture<Boolean> migrateEdgeEvents() {
        return Futures.immediateFuture(Boolean.FALSE);
    }

}
