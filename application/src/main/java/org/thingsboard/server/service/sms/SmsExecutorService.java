// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AbstractListeningExecutor;

@Component
public class SmsExecutorService extends AbstractListeningExecutor {

    @Value("${actors.rule.sms_thread_pool_size}")
    private int smsExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return smsExecutorThreadPoolSize;
    }

}
