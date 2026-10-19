// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;

import java.util.Base64;
import java.util.HashMap;

@Slf4j
public class ScriptUplinkEvaluator extends AbstractScriptEvaluator {

    public ScriptUplinkEvaluator(TenantId tenantId, ScriptInvokeService invokeService, EntityId entityId, String script) {
        super(tenantId, invokeService, entityId, ScriptType.UPLINK_CONVERTER_SCRIPT, script);
    }

    public ListenableFuture<String> execute(byte[] data, UplinkMetaData metadata) throws Exception {
        validateSuccessfulScriptLazyInit();
        Object[] inArgs = prepareArgs(scriptInvokeService.getLanguage(), data, metadata);
        return Futures.transform(scriptInvokeService.invokeScript(tenantId, null, this.scriptId, inArgs[0], inArgs[1]),
                eval -> {
                    if (eval instanceof String) {
                        return eval.toString();
                    } else {
                        return JacksonUtil.toString(eval);
                    }
                }, MoreExecutors.directExecutor());
    }

    private static Object[] prepareArgs(ScriptLanguage scriptLang, byte[] data, UplinkMetaData metadata) {
        if (ScriptLanguage.JS.equals(scriptLang)) {
            try {
                String[] args = new String[2];
                args[0] = Base64.getEncoder().encodeToString(data);
                args[1] = JacksonUtil.toString(metadata.getKvMap());
                return args;
            } catch (Throwable th) {
                throw new IllegalArgumentException("Cannot bind js args", th);
            }
        } else {
            Object[] args = new Object[2];
            args[0] = data;
            args[1] = new HashMap<>(metadata.getKvMap());
            return args;
        }
    }

    @Override
    protected String[] getArgNames() {
        return new String[]{"payload", "metadata"};
    }
}
