// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.common;

import org.thingsboard.common.util.ThingsBoardExecutors;

import java.util.concurrent.ExecutorService;

public abstract class LwM2MExecutorAwareService {

    protected ExecutorService executor;

    protected abstract int getExecutorSize();

    protected abstract String getExecutorName();

    protected void init() {
        this.executor = ThingsBoardExecutors.newWorkStealingPool(getExecutorSize(), getExecutorName());
    }

    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
