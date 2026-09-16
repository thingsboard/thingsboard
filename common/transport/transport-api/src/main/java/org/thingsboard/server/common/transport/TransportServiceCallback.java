// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

public interface TransportServiceCallback<T> {

    TransportServiceCallback<Void> EMPTY = new TransportServiceCallback<Void>() {
        @Override
        public void onSuccess(Void msg) {

        }

        @Override
        public void onError(Throwable e) {

        }
    };

    void onSuccess(T msg);

    void onError(Throwable e);

}
