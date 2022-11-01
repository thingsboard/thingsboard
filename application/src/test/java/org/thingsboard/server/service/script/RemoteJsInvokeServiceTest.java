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

import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        TbApiUsageStateService apiUsageStateService = mock(TbApiUsageStateService.class);
        ApiUsageState apiUsageState = mock(ApiUsageState.class);
        when(apiUsageState.isJsExecEnabled()).thenReturn(true);
        when(apiUsageStateService.getApiUsageState(any())).thenReturn(apiUsageState);
        TbApiUsageClient apiUsageClient = mock(TbApiUsageClient.class);

        remoteJsInvokeService = new RemoteJsInvokeService(apiUsageStateService, apiUsageClient);
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
    }

    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    @Test
    public void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
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
        Object invocationResult = remoteJsInvokeService.invokeFunction(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(jsInvokeRequestMade.getScriptHash()).isEqualTo(getScriptHash(scriptId));
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
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
        Object invocationResult = remoteJsInvokeService.invokeFunction(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
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
    public void whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody() throws Exception {
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
            UUID scriptUuid = remoteJsInvokeService.eval(tenantId1, JsScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
            tenant1Script1Hash = getScriptHash(scriptUuid);
            scriptHashes.add(tenant1Script1Hash);
        }
        assertThat(scriptHashes).as("Unique scripts ids").size().isOne();

        TenantId tenantId2 = TenantId.fromUUID(UUID.randomUUID());
        UUID scriptUuid = remoteJsInvokeService.eval(tenantId2, JsScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
        String tenant2Script1Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script1Id).isNotEqualTo(tenant1Script1Hash);

        String scriptBody2 = scriptBody1 + ";;";
        scriptUuid = remoteJsInvokeService.eval(tenantId2, JsScriptType.RULE_NODE_SCRIPT, scriptBody2).get();
        String tenant2Script2Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script2Id).isNotEqualTo(tenant2Script1Id);
    }

    @Test
    public void whenReleasingScript_thenCheckForHashUsages() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId1 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        UUID scriptId2 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
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
        return remoteJsInvokeService.scriptIdToNameAndHashMap.get(scriptUuid).getSecond();
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
