/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import delight.graaljssandbox.GraalSandbox;
import delight.graaljssandbox.GraalSandboxes;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.ExecutorService;

@Slf4j
@ConditionalOnExpression("'${js.evaluator:local}'=='local' && '${js.local.engine:nashorn}'=='graal'")
@Service
public class GraalJsInvokeService extends AbstractLocalJsInvokeService {

    private GraalSandbox sandbox;
    private ScriptEngine engine;

    public GraalJsInvokeService(TbApiUsageStateService apiUsageStateService, TbApiUsageClient apiUsageClient, JsExecutorService jsExecutor) {
        super(apiUsageStateService, apiUsageClient, jsExecutor);
    }

    @Override
    protected void initEngine() {
        engine = GraalJSScriptEngine.create(null,
                Context.newBuilder("js")
                        .allowAllAccess(false)
                        .option("js.ecmascript-version", "2021"));
    }

    @Override
    protected void initSandbox(ExecutorService monitorExecutorService) {
        sandbox = GraalSandboxes.create();
        sandbox.setExecutor(monitorExecutorService);
        sandbox.setMaxCPUTime(getMaxCpuTime());
        sandbox.allowNoBraces(false);
        sandbox.allowLoadFunctions(true);
        sandbox.setMaxPreparedStatements(30);
    }

    @Override
    protected void evalUsingEngine(String jsScript) throws ScriptException {
        engine.eval(jsScript);
    }

    @Override
    protected void evalUsingSandbox(String jsScript) throws ScriptException {
        sandbox.eval(jsScript);
    }

    @Override
    protected Object invokeSandboxFunction(String functionName, Object[] args) throws ScriptException, NoSuchMethodException {
        return sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
    }

    @Override
    protected Object invokeEngineFunction(String functionName, Object[] args) throws ScriptException, NoSuchMethodException {
        return ((Invocable) engine).invokeFunction(functionName, args);
    }

}
