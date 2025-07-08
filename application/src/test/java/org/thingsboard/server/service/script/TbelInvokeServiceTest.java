/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.mvel2.CompileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.script.api.tbel.TbelScript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@TestPropertySource(properties = {
        "tbel.max_script_body_size=100",
        "tbel.max_total_args_size=50",
        "tbel.max_result_size=50",
        "tbel.max_errors=2",
        "tbel.compiled_scripts_cache_size=100"
})
class TbelInvokeServiceTest extends AbstractTbelInvokeTest {

    @Value("${tbel.max_errors}")
    private int maxJsErrors;

    @Test
    void givenUncompilableScript_whenEvaluating_thenThrowsErrorWithCompilationErrorCode() {
        // GIVEN
        var uncompilableScript = "return msg.property !== undefined;";

        // WHEN-THEN
        assertThatThrownBy(() -> evalScript(uncompilableScript))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(TbScriptException.class)
                .asInstanceOf(type(TbScriptException.class))
                .satisfies(ex -> {
                    assertThat(ex.getScriptId()).isNotNull();
                    assertThat(ex.getErrorCode()).isEqualTo(TbScriptException.ErrorCode.COMPILATION);
                    assertThat(ex.getBody()).isEqualTo(uncompilableScript);
                    assertThat(ex.getCause()).isInstanceOf(CompileException.class);
                });
    }

    @Test
    void givenSimpleScriptTestPerformance() throws ExecutionException, InterruptedException {
        int iterations = 100000;
        UUID scriptId = evalScript("return msg.temperature > 20");
        // warmup
        ObjectNode msg = JacksonUtil.newObjectNode();
        for (int i = 0; i < 100; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(invokeScriptResultString(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long startTs = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.parseBoolean(invokeScriptResultString(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long duration = System.currentTimeMillis() - startTs;
        System.out.println(iterations + " invocations took: " + duration + "ms");
        Assert.assertTrue(duration < TimeUnit.MINUTES.toMillis(1));
    }

    @Test
    void givenTooBigScriptForEval_thenReturnError() {
        String hugeScript = "var a = 'qwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababer'; return {a: a};";

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
                invokeScriptResultString(scriptId, hugeMsg);
            }).hasMessageContaining("input arguments exceed maximum");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    @Test
    void whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "var s = 'a'; for(int i=0; i<50; i++){ s +='a';} return { s: s};";
        UUID scriptId = evalScript(script);

        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScriptResultString(scriptId, "{}");
            }).hasMessageContaining("result exceeds maximum allowed size");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    @Test
    void givenScriptsWithSameBody_thenCompileAndCacheOnlyOnce() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = (Map<UUID, String>) ReflectionTestUtils.getField(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = (Map<String, TbelScript>) ReflectionTestUtils.getField(invokeService, "scriptMap");
        Cache<String, Serializable> compiledScriptsCache = (Cache<String, Serializable>) ReflectionTestUtils.getField(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));

        assertThat(scriptsIds.stream().map(scriptIdToHash::get)).containsOnly(scriptHash);
        assertThat(scriptMap).containsKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();
    }

    @Test
    public void whenReleasingScript_thenCheckForScriptHashUsages() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = (Map<UUID, String>) ReflectionTestUtils.getField(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = (Map<String, TbelScript>) ReflectionTestUtils.getField(invokeService, "scriptMap");
        Cache<String, Serializable> compiledScriptsCache = (Cache<String, Serializable>) ReflectionTestUtils.getField(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));
        for (int i = 0; i < 9; i++) {
            UUID scriptId = scriptsIds.get(i);
            assertThat(scriptIdToHash).containsKey(scriptId);
            invokeService.release(scriptId);
            assertThat(scriptIdToHash).doesNotContainKey(scriptId);
        }
        assertThat(scriptMap).containsKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();

        invokeService.release(scriptsIds.get(9));
        assertThat(scriptMap).doesNotContainKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNull();
    }

    @Test
    @Ignore("This test is based on assumption that Caffeine cache is LRU based but in fact it is based on " +
            "Tiny LFU which is the cause that the tests fail sometime: https://arxiv.org/pdf/1512.00727.pdf")
    public void whenCompiledScriptsCacheIsTooBig_thenRemoveRarelyUsedScripts() throws Exception {
        Map<UUID, String> scriptIdToHash = (Map<UUID, String>) ReflectionTestUtils.getField(invokeService, "scriptIdToHash");
        Cache<String, Serializable> compiledScriptsCache = (Cache<String, Serializable>) ReflectionTestUtils.getField(invokeService, "compiledScriptsCache");

        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 110; i++) { // tbel.compiled_scripts_cache_size = 100
            String script = "return msg.temperature > " + i;
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);

            for (int j = 0; j < i; j++) {
                invokeScriptResultString(scriptId, "{ \"temperature\": 12 }"); // so that scriptsIds is ordered by number of invocations
            }
        }

        ConcurrentMap<String, Serializable> cache = compiledScriptsCache.asMap();

        for (int i = 0; i < 10; i++) { // iterating rarely used scripts
            UUID scriptId = scriptsIds.get(i);
            String scriptHash = scriptIdToHash.get(scriptId);
            assertThat(cache).doesNotContainKey(scriptHash);
        }
        for (int i = 10; i < 110; i++) {
            UUID scriptId = scriptsIds.get(i);
            String scriptHash = scriptIdToHash.get(scriptId);
            assertThat(cache).containsKey(scriptHash);
        }

        UUID scriptRemovedFromCache = scriptsIds.get(0);
        assertThat(compiledScriptsCache.getIfPresent(scriptIdToHash.get(scriptRemovedFromCache))).isNull();
        invokeScriptResultString(scriptRemovedFromCache, "{ \"temperature\": 12 }");
        assertThat(compiledScriptsCache.getIfPresent(scriptIdToHash.get(scriptRemovedFromCache))).isNotNull();
    }

    private void assertThatScriptIsBlocked(UUID scriptId) {
        assertThatThrownBy(() -> {
            invokeScriptResultString(scriptId, "{}");
        }).hasMessageContaining("invocation is blocked due to maximum error");
    }
}
