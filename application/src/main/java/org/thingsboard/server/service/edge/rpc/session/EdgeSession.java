// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;

import java.io.Closeable;
import java.util.List;

public interface EdgeSession extends Closeable {

    StreamObserver<RequestMsg> initInputStream();
    EdgeSessionState getState();
    void startSyncProcess(boolean fullSync);
    void sendDownlinkMsg(ResponseMsg responseMsg);
    void addHighPriorityEvent(EdgeEvent edgeEvent);
    void processHighPriorityEvents();
    boolean hasHighPriorityEvents();
    ListenableFuture<Pair<Long, Long>> fetchAndSendEdgeEvents(EdgeEventFetcher fetcher);
    ListenableFuture<Boolean> sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack);

}
