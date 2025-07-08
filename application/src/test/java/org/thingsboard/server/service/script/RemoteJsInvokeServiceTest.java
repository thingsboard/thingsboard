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

import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RemoteJsInvokeServiceTest {

    private RemoteJsInvokeService remoteJsInvokeService;
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> jsRequestTemplate;

    @BeforeEach
    public void beforeEach() {
        TbApiUsageStateClient apiUsageStateClient = mock(TbApiUsageStateClient.class);
        ApiUsageState apiUsageState = mock(ApiUsageState.class);
        when(apiUsageState.isJsExecEnabled()).thenReturn(true);
        when(apiUsageStateClient.getApiUsageState(any())).thenReturn(apiUsageState);
        TbApiUsageReportClient apiUsageReportClient = mock(TbApiUsageReportClient.class);

        remoteJsInvokeService = new RemoteJsInvokeService(Optional.of(apiUsageStateClient), Optional.of(apiUsageReportClient));
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
        StatsFactory statsFactory = mock(StatsFactory.class);
        when(statsFactory.createStatsCounter(any(), any())).thenReturn(mock(StatsCounter.class));
        ReflectionTestUtils.setField(remoteJsInvokeService, "statsFactory", statsFactory);
        remoteJsInvokeService.init();
    }

    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    @Test
    void givenUncompilableScript_whenEvaluating_thenThrowsErrorWithCompilationErrorCode() {
        // GIVEN
        doAnswer(methodCall -> Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setCompileResponse(JsInvokeProtos.JsCompileResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorCode(JsInvokeProtos.JsInvokeErrorCode.COMPILATION_ERROR)
                        .setErrorDetails("SyntaxError: Unexpected token 'const'")
                        .setScriptHash(methodCall.<TbProtoQueueMsg<RemoteJsRequest>>getArgument(0).getValue().getCompileRequest().getScriptHash())
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> jsQueueMsg.getValue().hasCompileRequest()));

        var uncompilableScript = "let const = 'this is not allowed';";

        // WHEN-THEN
        assertThatThrownBy(() -> remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, uncompilableScript).get())
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(TbScriptException.class)
                .asInstanceOf(type(TbScriptException.class))
                .satisfies(ex -> {
                    assertThat(ex.getScriptId()).isNotNull();
                    assertThat(ex.getErrorCode()).isEqualTo(TbScriptException.ErrorCode.COMPILATION);
                    assertThat(ex.getBody()).contains(uncompilableScript);
                    assertThat(ex.getCause()).isInstanceOf(RuntimeException.class).hasMessage("SyntaxError: Unexpected token 'const'");
                });
    }

    @Test
    void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        String expectedInvocationResult = "scriptInvocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(jsInvokeRequestMade.getScriptHash()).isEqualTo(getScriptHash(scriptId));
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorCode(JsInvokeProtos.JsInvokeErrorCode.NOT_FOUND_ERROR)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        String expectedInvocationResult = "invocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isNotEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestsCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate, times(2)).send(jsRequestsCaptor.capture());

        List<TbProtoJsQueueMsg<RemoteJsRequest>> jsInvokeRequestsMade = jsRequestsCaptor.getAllValues();

        JsInvokeProtos.JsInvokeRequest firstRequestMade = jsInvokeRequestsMade.get(0).getValue().getInvokeRequest();
        assertThat(firstRequestMade.getScriptBody()).isNullOrEmpty();

        JsInvokeProtos.JsInvokeRequest secondRequestMade = jsInvokeRequestsMade.get(1).getValue().getInvokeRequest();
        assertThat(secondRequestMade.getScriptBody()).contains(scriptBody);

        assertThat(jsInvokeRequestsMade.stream().map(TbProtoQueueMsg::getKey).distinct().count()).as("partition keys are same")
                .isOne();

        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    void whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody() throws Exception {
        mockJsEvalResponse();

        TenantId tenantId1 = TenantId.fromUUID(UUID.randomUUID());
        String scriptBody1 = "var msg = { temp: 42, humidity: 77 };\n" +
                "var metadata = { data: 40 };\n" +
                "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                "\n" +
                "return { msg: msg, metadata: metadata, msgType: msgType };";

        Set<String> scriptHashes = new HashSet<>();
        String tenant1Script1Hash = null;
        for (int i = 0; i < 3; i++) {
            UUID scriptUuid = remoteJsInvokeService.eval(tenantId1, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
            tenant1Script1Hash = getScriptHash(scriptUuid);
            scriptHashes.add(tenant1Script1Hash);
        }
        assertThat(scriptHashes).as("Unique scripts ids").size().isOne();

        TenantId tenantId2 = TenantId.fromUUID(UUID.randomUUID());
        UUID scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
        String tenant2Script1Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script1Id).isNotEqualTo(tenant1Script1Hash);

        String scriptBody2 = scriptBody1 + ";;";
        scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody2).get();
        String tenant2Script2Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script2Id).isNotEqualTo(tenant2Script1Id);
    }

    @Test
    void whenReleasingScript_thenCheckForHashUsages() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId1 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        UUID scriptId2 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        String scriptHash = getScriptHash(scriptId1);
        assertThat(scriptHash).isEqualTo(getScriptHash(scriptId2));
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setReleaseResponse(JsInvokeProtos.JsReleaseResponse.newBuilder()
                        .setSuccess(true)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        remoteJsInvokeService.release(scriptId1).get();
        verifyNoInteractions(jsRequestTemplate);
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).containsKey(scriptHash);

        remoteJsInvokeService.release(scriptId2).get();
        verify(jsRequestTemplate).send(any());
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).isEmpty();
    }

    private String getScriptHash(UUID scriptUuid) {
        return remoteJsInvokeService.getScriptHash(scriptUuid);
    }

    private void mockJsEvalResponse() {
        doAnswer(methodCall -> Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setCompileResponse(JsInvokeProtos.JsCompileResponse.newBuilder()
                        .setSuccess(true)
                        .setScriptHash(methodCall.<TbProtoQueueMsg<RemoteJsRequest>>getArgument(0).getValue().getCompileRequest().getScriptHash())
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> jsQueueMsg.getValue().hasCompileRequest()));
    }

}
