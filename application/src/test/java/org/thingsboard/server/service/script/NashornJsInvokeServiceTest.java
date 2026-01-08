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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.script.api.js.NashornJsInvokeService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

@DaoSqlTest
@TestPropertySource(properties = {
        "js.evaluator=local",
        "js.max_script_body_size=10000",
        "js.max_total_args_size=50",
        "js.max_result_size=50",
        "js.local.max_errors=2",
})
@Slf4j
class NashornJsInvokeServiceTest extends AbstractControllerTest {

    @Autowired
    private NashornJsInvokeService invokeService;

    @Value("${js.local.max_errors}")
    private int maxJsErrors;

    @Test
    void givenUncompilableScript_whenEvaluating_thenThrowsErrorWithCompilationErrorCode() {
        // GIVEN
        var uncompilableScript = "return msg.temperature?.value;";

        // WHEN-THEN
        assertThatThrownBy(() -> evalScript(uncompilableScript))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(TbScriptException.class)
                .asInstanceOf(type(TbScriptException.class))
                .satisfies(ex -> {
                    assertThat(ex.getScriptId()).isNotNull();
                    assertThat(ex.getErrorCode()).isEqualTo(TbScriptException.ErrorCode.COMPILATION);
                    assertThat(ex.getBody()).contains(uncompilableScript);
                    assertThat(ex.getCause()).isInstanceOf(ScriptException.class);
                });
    }

    @Test
    void givenSimpleScriptTestPerformance() throws ExecutionException, InterruptedException {
        int iterations = 1000;
        UUID scriptId = evalScript("return msg.temperature > 20");
        // warmup
        log.info("Warming up 1000 times...");
        var warmupWatch = TbStopWatch.create();
        for (int i = 0; i < 1000; i++) {
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(invokeScript(scriptId, "{\"temperature\":" + i + "}"));
            Assert.assertEquals(expected, result);
        }
        log.info("Warming up finished in {} ms", warmupWatch.stopAndGetTotalTimeMillis());
        log.info("Starting performance test...");
        var watch = TbStopWatch.create();
        for (int i = 0; i < iterations; i++) {
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(invokeScript(scriptId, "{\"temperature\":" + i + "}"));
            log.debug("asserting result");
            Assert.assertEquals(expected, result);
        }
        long duration = watch.stopAndGetTotalTimeMillis();
        log.info("Performance test with {} invocations took: {} ms", iterations, duration);
        assertThat(duration).as("duration ms")
                .isLessThan(TimeUnit.MINUTES.toMillis(1)); // effective exec time is about 500ms
    }

    @Test
    void givenSimpleScriptMultiThreadTestPerformance() throws ExecutionException, InterruptedException, TimeoutException {
        int iterations = 1000 * 4;
        List<ListenableFuture<Object>> futures = new ArrayList<>(iterations);
        UUID scriptId = evalScript("return msg.temperature > 20 ;");
        // warmup
        log.info("Warming up 1000 times...");

        var warmupWatch = TbStopWatch.create();
        for (int i = 0; i < 1000; i++) {
            futures.add(invokeScriptAsync(scriptId, "{\"temperature\":" + i + "}"));
        }
        List<Object> results = Futures.allAsList(futures).get(1, TimeUnit.MINUTES);
        for (int i = 0; i < 1000; i++) {
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(results.get(i).toString());
            Assert.assertEquals(expected, result);
        }
        log.info("Warming up finished in {} ms", warmupWatch.stopAndGetTotalTimeMillis());
        futures.clear();

        log.info("Starting performance test...");
        var watch = TbStopWatch.create();
        for (int i = 0; i < iterations; i++) {
            futures.add(invokeScriptAsync(scriptId, "{\"temperature\":" + i + "}"));
        }
        results = Futures.allAsList(futures).get(1, TimeUnit.MINUTES);
        for (int i = 0; i < iterations; i++) {
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(results.get(i).toString());
            Assert.assertEquals(expected, result);
        }
        long duration = watch.stopAndGetTotalTimeMillis();
        log.info("Performance test with {} invocations took: {} ms", iterations, duration);
        assertThat(duration).as("duration ms")
                .isLessThan(TimeUnit.MINUTES.toMillis(1)); // effective exec time is about 500ms
    }

    @Test
    void givenTooBigScriptForEval_thenReturnError() {
        String hugeScript = "var a = '" + "a".repeat(10000) + "'; return {a: a};";

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

    @Test
    void givenComplexScript_testCompile() {
        String script = """
                function(data) {
                  if (data.get("propertyA") == "a special value 1" || data.get("propertyA") == "a special value 2") {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyC") == "a special value 1" || data.get("propertyJ") == "a special value 1" || data.get("propertyV") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "4" && (data.get("propertyD") == "a special value 1" || data.get("propertyV") == "a special value 1" || data.get("propertyW") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 2" && (data.get("propertyE") == "a special value 1" || data.get("propertyF") == "a special value 1" || data.get("propertyL") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyE") == "a special value 1" || data.get("propertyF") == "a special value 1" || data.get("propertyL") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  }  else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else if (data.get("propertyB") == "a special value 3" && (data.get("propertyM") == "a special value 1" || data.get("propertyY") == "a special value 1" || data.get("propertyH") == "a special value 1")) {
                    return "a special value 1";
                  } else {
                     return "0"
                  };
                }
                """;

        // with delight-nashorn-sandbox 0.4.2, this would throw delight.nashornsandbox.exceptions.ScriptCPUAbuseException: Regular expression running for too many iterations. The operation could NOT be gracefully interrupted.
        assertDoesNotThrow(() -> {
            evalScript(script);
        });
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
        return invokeScriptAsync(scriptId, msg).get().toString();
    }

    private ListenableFuture<Object> invokeScriptAsync(UUID scriptId, String msg) {
        return invokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, msg, "{}", POST_TELEMETRY_REQUEST.name());
    }

}
