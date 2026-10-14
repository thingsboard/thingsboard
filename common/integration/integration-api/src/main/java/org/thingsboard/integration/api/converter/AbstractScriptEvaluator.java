// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class AbstractScriptEvaluator {

    protected final ScriptInvokeService scriptInvokeService;
    private final ScriptType scriptType;
    private final String script;
    protected final TenantId tenantId;
    protected final EntityId entityId;

    protected volatile UUID scriptId;
    private volatile boolean isErrorScript = false;


    public AbstractScriptEvaluator(TenantId tenantId, ScriptInvokeService scriptInvokeService, EntityId entityId, ScriptType scriptType, String script) {
        this.scriptInvokeService = scriptInvokeService;
        this.scriptType = scriptType;
        this.script = script;
        this.tenantId = tenantId;
        this.entityId = entityId;
    }

    public void destroy() {
        if (this.scriptId != null) {
            this.scriptInvokeService.release(this.scriptId);
        }
    }

    void validateSuccessfulScriptLazyInit() {
        if (this.scriptId != null) {
            return;
        }

        if (isErrorScript) {
            throw new IllegalArgumentException("Can't compile uplink converter script ");
        }

        synchronized (this) {
            if (this.scriptId == null) {
                try {
                    this.scriptId = this.scriptInvokeService.eval(tenantId, scriptType, script, this.getArgNames()).get();
                } catch (Exception e) {
                    if (!(e.getCause() instanceof TimeoutException)) {
                        isErrorScript = true;
                    }
                    throw new IllegalArgumentException("Can't compile script: " + e.getMessage(), e);
                }
            }
        }
    }

    protected abstract String[] getArgNames();

}
