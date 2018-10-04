/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.ListenableFuture;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;

import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestNashornJsSandboxService extends AbstractNashornJsSandboxService {

    private boolean useJsSandbox;
    private final int monitorThreadPoolSize;
    private final long maxCpuTime;
    private final int maxErrors;

    public TestNashornJsSandboxService(boolean useJsSandbox, int monitorThreadPoolSize, long maxCpuTime, int maxErrors) {
        this.useJsSandbox = useJsSandbox;
        this.monitorThreadPoolSize = monitorThreadPoolSize;
        this.maxCpuTime = maxCpuTime;
        this.maxErrors = maxErrors;
        init();
    }

    @Override
    protected boolean useJsSandbox() {
        return useJsSandbox;
    }

    @Override
    protected int getMonitorThreadPoolSize() {
        return monitorThreadPoolSize;
    }

    @Override
    protected long getMaxCpuTime() {
        return maxCpuTime;
    }

    @Override
    protected int getMaxErrors() {
        return maxErrors;
    }
}
