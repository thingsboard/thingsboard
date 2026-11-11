// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Slf4j
public abstract class RuleNodeScriptEngine<T extends ScriptInvokeService, R> implements ScriptEngine {

    private final T scriptInvokeService;

    protected final UUID scriptId;
    private final TenantId tenantId;

    public RuleNodeScriptEngine(TenantId tenantId, T scriptInvokeService, ScriptType scriptType, String script, String... argNames) {
        this.tenantId = tenantId;
        this.scriptInvokeService = scriptInvokeService;
        try {
            scriptId = this.scriptInvokeService.eval(tenantId, scriptType, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            if (t instanceof TbScriptException scriptException) {
                throw scriptException;
            }
            throw new RuntimeException("Unexpected error when creating script engine: " + t.getMessage(), t);
        }
    }

    protected abstract Object[] prepareArgs(TbMsg msg);

    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<R> result = executeScriptAsync(msg);
        return Futures.transform(result, json -> executeUpdateTransform(msg, json), directExecutor());
    }

    protected abstract List<TbMsg> executeUpdateTransform(TbMsg msg, R result);

    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg) {
        return Futures.transform(executeScriptAsync(prevMsg), result -> executeGenerateTransform(prevMsg, result), directExecutor());
    }

    protected abstract TbMsg executeGenerateTransform(TbMsg prevMsg, R result);

    @Override
    public ListenableFuture<Boolean> executeAttributesFilterAsync(Map<String, KvEntry> attributes) {
        Object inArgs = prepareAttributes(attributes);
        return Futures.transform(executeScriptAsync(null, inArgs), this::executeFilterTransform, directExecutor());
    }

    protected abstract Object prepareAttributes(Map<String, KvEntry> attributes);

    @Override
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeFilterTransform, directExecutor());
    }

    protected abstract boolean executeFilterTransform(R result);

    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeSwitchTransform, directExecutor()); // usually runs on a callbackExecutor
    }

    protected abstract Set<String> executeSwitchTransform(R result);

    @Override
    public ListenableFuture<String> executeToStringAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), this::executeToStringTransform, directExecutor());
    }

    protected abstract String executeToStringTransform(R result);

    ListenableFuture<R> executeScriptAsync(TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        Object[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    private ListenableFuture<R> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transform(scriptInvokeService.invokeScript(tenantId, customerId, scriptId, args), this::convertResult, directExecutor());
    }

    public void destroy() {
        scriptInvokeService.release(scriptId);
    }

    protected abstract R convertResult(Object result);

}
