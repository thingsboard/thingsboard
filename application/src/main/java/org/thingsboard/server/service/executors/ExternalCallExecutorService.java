// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AbstractListeningExecutor;

@Component
public class ExternalCallExecutorService extends AbstractListeningExecutor {

    @Value("${actors.rule.external_call_thread_pool_size}")
    private int externalCallExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return externalCallExecutorThreadPoolSize;
    }

}

