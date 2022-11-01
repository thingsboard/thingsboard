/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.ScriptException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
public abstract class RuleNodeScriptEngine<T extends ScriptInvokeService, R> implements ScriptEngine {

    private final T scriptInvokeService;

    private final UUID scriptId;
    private final TenantId tenantId;

    public RuleNodeScriptEngine(TenantId tenantId, T scriptInvokeService, String script, String... argNames) {
        this.tenantId = tenantId;
        this.scriptInvokeService = scriptInvokeService;
        try {
            this.scriptId = this.scriptInvokeService.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
        }
    }

    protected abstract Object[] prepareArgs(TbMsg msg);

    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<R> result = executeScriptAsync(msg);
        return Futures.transformAsync(result,
                json -> executeUpdateTransform(msg, json),
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, R result);

    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg) {
        return Futures.transformAsync(executeScriptAsync(prevMsg),
                result -> executeGenerateTransform(prevMsg, result),
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, R result);

    @Override
    public ListenableFuture<String> executeToStringAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg), this::executeToStringTransform, MoreExecutors.directExecutor());
    }


    @Override
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeFilterTransform,
                MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<String> executeToStringTransform(R result);

    protected abstract ListenableFuture<Boolean> executeFilterTransform(R result);

    protected abstract ListenableFuture<Set<String>> executeSwitchTransform(R result);

    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeSwitchTransform,
                MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    ListenableFuture<R> executeScriptAsync(TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        Object[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    ListenableFuture<R> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transformAsync(scriptInvokeService.invokeScript(tenantId, customerId, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(convertResult(o));
                    } catch (Exception e) {
                        if (e.getCause() instanceof ScriptException) {
                            return Futures.immediateFailedFuture(e.getCause());
                        } else if (e.getCause() instanceof RuntimeException) {
                            return Futures.immediateFailedFuture(new ScriptException(e.getCause().getMessage()));
                        } else {
                            return Futures.immediateFailedFuture(new ScriptException(e));
                        }
                    }
                }, MoreExecutors.directExecutor());
    }

    public void destroy() {
        scriptInvokeService.release(this.scriptId);
    }

    protected abstract R convertResult(Object result);
}
