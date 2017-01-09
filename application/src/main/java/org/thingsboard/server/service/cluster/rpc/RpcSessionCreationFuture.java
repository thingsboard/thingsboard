/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cluster.rpc;

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.util.concurrent.*;

/**
 * @author Andrew Shvayka
 */
public class RpcSessionCreationFuture implements Future<StreamObserver<ClusterAPIProtos.ToRpcServerMessage>> {

    private final BlockingQueue<StreamObserver<ClusterAPIProtos.ToRpcServerMessage>> queue = new ArrayBlockingQueue<>(1);

    public void onMsg(StreamObserver<ClusterAPIProtos.ToRpcServerMessage> result) throws InterruptedException {
        queue.put(result);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ToRpcServerMessage> get() throws InterruptedException, ExecutionException {
        return this.queue.take();
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ToRpcServerMessage> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        StreamObserver<ClusterAPIProtos.ToRpcServerMessage> result = this.queue.poll(timeout, unit);
        if (result == null) {
            throw new TimeoutException();
        } else {
            return result;
        }
    }
}
