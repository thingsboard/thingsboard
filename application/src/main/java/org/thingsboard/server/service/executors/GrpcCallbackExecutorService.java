// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AbstractListeningExecutor;

@Component
public class GrpcCallbackExecutorService extends AbstractListeningExecutor {

    @Value("${edges.grpc_callback_thread_pool_size}")
    private int grpcCallbackExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return grpcCallbackExecutorThreadPoolSize;
    }

}
