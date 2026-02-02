/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
