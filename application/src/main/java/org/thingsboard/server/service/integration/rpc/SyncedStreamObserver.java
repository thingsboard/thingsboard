// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import io.grpc.stub.StreamObserver;

public class SyncedStreamObserver<V> implements StreamObserver<V> {
    private final StreamObserver<V> delegate;

    public SyncedStreamObserver(StreamObserver<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onNext(V value) {
        synchronized (delegate) {
            delegate.onNext(value);
        }
    }

    @Override
    public void onError(Throwable t) {
        synchronized (delegate) {
            delegate.onError(t);
        }
    }

    @Override
    public void onCompleted() {
        synchronized (delegate) {
            delegate.onCompleted();
        }
    }
}
