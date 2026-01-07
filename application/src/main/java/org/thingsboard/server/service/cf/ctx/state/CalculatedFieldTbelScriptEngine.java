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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;

import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CalculatedFieldTbelScriptEngine implements CalculatedFieldScriptEngine {

    private final TbelInvokeService tbelInvokeService;

    private final UUID scriptId;
    private final TenantId tenantId;

    public CalculatedFieldTbelScriptEngine(TenantId tenantId, TbelInvokeService tbelInvokeService, String script, String... argNames) {
        this.tenantId = tenantId;
        this.tbelInvokeService = tbelInvokeService;
        try {
            this.scriptId = this.tbelInvokeService.eval(tenantId, ScriptType.CALCULATED_FIELD_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
        }
    }

    @Override
    public ListenableFuture<Object> executeScriptAsync(Object[] args) {
        log.trace("Executing script async, args {}", args);
        return Futures.transformAsync(tbelInvokeService.invokeScript(tenantId, null, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(o);
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

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(Object[] args) {
        return Futures.transform(executeScriptAsync(args), JacksonUtil::valueToTree, MoreExecutors.directExecutor());
    }

    @Override
    public void destroy() {
        tbelInvokeService.release(this.scriptId);
    }
}
