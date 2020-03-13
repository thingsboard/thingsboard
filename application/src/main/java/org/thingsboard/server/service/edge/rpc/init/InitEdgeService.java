package org.thingsboard.server.service.edge.rpc.init;

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.gen.edge.ResponseMsg;

public interface InitEdgeService {

    void init(Edge edge, StreamObserver<ResponseMsg> outputStream);
}
