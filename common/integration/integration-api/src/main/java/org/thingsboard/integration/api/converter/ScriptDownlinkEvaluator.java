// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ScriptDownlinkEvaluator extends AbstractScriptEvaluator {

    public ScriptDownlinkEvaluator(TenantId tenantId, ScriptInvokeService scriptInvokeService, EntityId entityId, String script) {
        super(tenantId, scriptInvokeService, entityId, ScriptType.DOWNLINK_CONVERTER_SCRIPT, script);
    }

    public JsonNode execute(TbMsg msg, IntegrationMetaData metadata) throws ScriptException {
        try {
            validateSuccessfulScriptLazyInit();
            Object[] inArgs = prepareArgs(scriptInvokeService.getLanguage(), msg, metadata);
            Object eval = scriptInvokeService.invokeScript(this.tenantId, msg.getCustomerId(), this.scriptId, inArgs[0], inArgs[1], inArgs[2], inArgs[3]).get();
            if (eval instanceof String) {
                return JacksonUtil.toJsonNode(eval.toString());
            } else {
                return JacksonUtil.valueToTree(eval);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException) e.getCause();
            } else {
                throw new ScriptException("Failed to execute js script: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ScriptException("Failed to execute js script: " + e.getMessage());
        }
    }

    private static Object[] prepareArgs(ScriptLanguage scriptLang, TbMsg msg, IntegrationMetaData metadata) {
        if (ScriptLanguage.JS.equals(scriptLang)) {
            try {
                String[] args = new String[4];
                if (msg.getData() != null) {
                    args[0] = msg.getData();
                } else {
                    args[0] = "";
                }
                args[1] = JacksonUtil.toString(msg.getMetaData().getData());
                args[2] = msg.getType();
                args[3] = JacksonUtil.toString(metadata.getKvMap());
                return args;
            } catch (Throwable th) {
                throw new IllegalArgumentException("Cannot bind js args", th);
            }
        } else {
            Object[] args = new Object[4];
            if (msg.getData() != null) {
                args[0] = JacksonUtil.fromString(msg.getData(), Map.class);
            } else {
                args[0] = new HashMap<>();
            }
            args[1] = new HashMap<>(msg.getMetaData().getData());
            args[2] = msg.getType();
            args[3] = new HashMap<>(metadata.getKvMap());
            return args;
        }
    }

    @Override
    protected String[] getArgNames() {
        return new String[]{"msg", "metadata", "msgType", "integrationMetadata"};
    }
}
