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

import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.ScriptException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class NashornJsSandboxService implements JsSandboxService {

    @Value("${actors.rule.js_sandbox.monitor_thread_pool_size}")
    private int monitorThreadPoolSize;

    @Value("${actors.rule.js_sandbox.max_cpu_time}")
    private long maxCpuTime;

    private NashornSandbox sandbox = NashornSandboxes.create();
    private ExecutorService monitorExecutorService;

    @PostConstruct
    public void init() {
        monitorExecutorService = Executors.newFixedThreadPool(monitorThreadPoolSize);
        sandbox.setExecutor(monitorExecutorService);
        sandbox.setMaxCPUTime(maxCpuTime);
        sandbox.allowNoBraces(false);
        sandbox.setMaxPreparedStatements(30);
    }

    @PreDestroy
    public void stop() {
        if  (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    @Override
    public Object eval(String js) throws ScriptException {
        return sandbox.eval(js);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return sandbox.getSandboxedInvocable().invokeFunction(name, args);
    }

}
