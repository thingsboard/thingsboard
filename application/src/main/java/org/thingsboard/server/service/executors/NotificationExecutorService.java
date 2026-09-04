// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AbstractListeningExecutor;

@Component
public class NotificationExecutorService extends AbstractListeningExecutor {

    @Value("${notification_system.thread_pool_size:10}")
    private int threadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return threadPoolSize;
    }

}
