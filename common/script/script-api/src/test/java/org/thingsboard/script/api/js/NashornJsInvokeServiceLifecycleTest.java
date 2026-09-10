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
package org.thingsboard.script.api.js;

import delight.nashornsandbox.NashornSandbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsFactory;

import javax.script.ScriptEngine;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Covers the JS global scope lifecycle against both backends the service can be configured with.
 * The Spring-wired {@code NashornJsInvokeServiceTest} only exercises the sandbox, since
 * {@code js.local.use_js_sandbox} defaults to true.
 */
class NashornJsInvokeServiceLifecycleTest {

    private static final String[] ARG_NAMES = {"msg", "metadata", "msgType"};

    private NashornJsInvokeService service;
    private boolean useJsSandbox;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
    }

    @ParameterizedTest(name = "useJsSandbox = {0}")
    @ValueSource(booleans = {true, false})
    void whenScriptIsReleased_thenGlobalPropertyIsRemovedAndFunctionIsGone(boolean useJsSandbox) throws Exception {
        givenService(useJsSandbox);
        UUID scriptId = UUID.randomUUID();

        JsScriptInfo scriptInfo = eval(scriptId, "return {doubled: msg.temperature * 2};");
        assertThat(hasGlobalProperty(scriptInfo)).isTrue();
        assertThat(invoke(scriptId, scriptInfo, "{\"temperature\":21}")).asString().contains("\"doubled\":42");

        service.doRelease(scriptId);

        assertThat(hasGlobalProperty(scriptInfo)).isFalse();
        assertThatThrownBy(() -> invoke(scriptId, scriptInfo, "{\"temperature\":21}"))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(TbScriptException.class);
    }

    @ParameterizedTest(name = "useJsSandbox = {0}")
    @ValueSource(booleans = {true, false})
    void whenOneOfTwoScriptsIsReleased_thenTheOtherKeepsWorking(boolean useJsSandbox) throws Exception {
        givenService(useJsSandbox);
        UUID releasedId = UUID.randomUUID();
        UUID retainedId = UUID.randomUUID();

        JsScriptInfo released = eval(releasedId, "return {v: 1};");
        JsScriptInfo retained = eval(retainedId, "return {v: 2};");

        service.doRelease(releasedId);

        assertThat(hasGlobalProperty(released)).isFalse();
        assertThat(hasGlobalProperty(retained)).isTrue();
        assertThat(invoke(retainedId, retained, "{}")).asString().contains("\"v\":2");
    }

    private void givenService(boolean useJsSandbox) {
        this.useJsSandbox = useJsSandbox;
        service = new NashornJsInvokeService(Optional.empty(), Optional.empty());
        ReflectionTestUtils.setField(service, "statsFactory", mock(StatsFactory.class, Mockito.RETURNS_DEEP_STUBS));
        ReflectionTestUtils.setField(service, "useJsSandbox", useJsSandbox);
        ReflectionTestUtils.setField(service, "jsExecutorThreadPoolSize", 4);
        ReflectionTestUtils.setField(service, "monitorThreadPoolSize", 2);
        ReflectionTestUtils.setField(service, "maxCpuTime", 8000L);
        ReflectionTestUtils.setField(service, "maxMemory", 104857600L);
        service.init();
    }

    private JsScriptInfo eval(UUID scriptId, String scriptBody) throws Exception {
        service.doEvalScript(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody, scriptId, ARG_NAMES).get();
        return service.scriptInfoMap.get(scriptId);
    }

    private Object invoke(UUID scriptId, JsScriptInfo scriptInfo, String msg) throws Exception {
        return service.doInvokeFunction(scriptId, scriptInfo, new Object[]{msg, "{}", "POST_TELEMETRY_REQUEST"}).get();
    }

    private boolean hasGlobalProperty(JsScriptInfo scriptInfo) throws Exception {
        String expression = "this.hasOwnProperty('" + scriptInfo.getFunctionName() + "')";
        Object result = useJsSandbox
                ? ((NashornSandbox) ReflectionTestUtils.getField(service, "sandbox")).eval(expression)
                : ((ScriptEngine) ReflectionTestUtils.getField(service, "engine")).eval(expression);
        return Boolean.TRUE.equals(result);
    }

}
