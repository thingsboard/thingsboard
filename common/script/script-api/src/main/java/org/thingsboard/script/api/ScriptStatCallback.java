// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;

import java.util.concurrent.TimeoutException;

@Slf4j
@AllArgsConstructor
public class ScriptStatCallback<T> implements FutureCallback<T> {

    private final StatsCounter successMsgs;
    private final StatsCounter timeoutMsgs;
    private final StatsCounter failedMsgs;

    @Override
    public void onSuccess(@Nullable T result) {
        successMsgs.increment();
    }

    @Override
    public void onFailure(Throwable t) {
        if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
            timeoutMsgs.increment();
        } else {
            failedMsgs.increment();
        }
    }
}
