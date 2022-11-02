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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.js.NashornJsInvokeService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
@TestPropertySource(properties = {
        "js.max_script_body_size=50",
        "js.max_total_args_size=50",
        "js.max_result_size=50",
        "js.local.max_errors=2"
})
class NashornJsInvokeServiceTest extends AbstractControllerTest {

    @Autowired
    private NashornJsInvokeService invokeService;

    @Value("${js.local.max_errors}")
    private int maxJsErrors;

    @Test
    void givenSimpleScriptTestPerformance() throws ExecutionException, InterruptedException {
        int iterations = 1000;
        UUID scriptId = evalScript("return msg.temperature > 20");
        // warmup
        ObjectNode msg = JacksonUtil.newObjectNode();
        for (int i = 0; i < 100; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.valueOf(invokeScript(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.valueOf(invokeScript(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long duration = System.currentTimeMillis() - startTs;
        System.out.println(iterations + " invocations took: " + duration + "ms");
        Assert.assertTrue(duration < TimeUnit.MINUTES.toMillis(3));
    }

    @Test
    void givenTooBigScriptForEval_thenReturnError() {
        String hugeScript = "var a = 'qwertyqwertywertyqwabababer'; return {a: a};";

        assertThatThrownBy(() -> {
            evalScript(hugeScript);
        }).hasMessageContaining("body exceeds maximum allowed size");
    }

    @Test
    void givenTooBigScriptInputArgs_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "return { msg: msg };";
        String hugeMsg = "{\"input\":\"123456781234349\"}";
        UUID scriptId = evalScript(script);

        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, hugeMsg);
            }).hasMessageContaining("input arguments exceed maximum");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    @Test
    void whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "var s = new Array(50).join('a'); return { s: s};";
        UUID scriptId = evalScript(script);

        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, "{}");
            }).hasMessageContaining("result exceeds maximum allowed size");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    private void assertThatScriptIsBlocked(UUID scriptId) {
        assertThatThrownBy(() -> {
            invokeScript(scriptId, "{}");
        }).hasMessageContaining("invocation is blocked due to maximum error");
    }

    private UUID evalScript(String script) throws ExecutionException, InterruptedException {
        return invokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, script).get();
    }

    private String invokeScript(UUID scriptId, String msg) throws ExecutionException, InterruptedException {
        return invokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, msg, "{}", "POST_TELEMETRY_REQUEST").get().toString();
    }

}
