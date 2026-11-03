// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.script;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.DefaultTbelInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.DefaultStatsFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

@SpringBootTest(classes = {SimpleMeterRegistry.class, DefaultStatsFactory.class, DefaultTbelInvokeService.class })
public abstract class AbstractTbelInvokeTest {

    @Autowired
    protected TbelInvokeService invokeService;

    protected UUID evalScript(String script) throws ExecutionException, InterruptedException {
        return invokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, script, "msg", "metadata", "msgType").get();
    }

    protected String invokeScriptResultString(UUID scriptId, String str) throws ExecutionException, InterruptedException {
        var msg = JacksonUtil.fromString(str, Map.class);
        return invokeScript(scriptId, str).toString();
    }
    protected Object invokeScript(UUID scriptId, String str) throws ExecutionException, InterruptedException {
        var msg = JacksonUtil.fromString(str, Map.class);
        return invokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, msg, "{}", POST_TELEMETRY_REQUEST.name()).get();
     }
}
