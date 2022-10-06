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
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RemoteJsInvokeServiceTest {

    private RemoteJsInvokeService remoteJsInvokeService;
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> jsRequestTemplate;


    @BeforeEach
    public void beforeEach() {
        remoteJsInvokeService = new RemoteJsInvokeService(null, null);
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
    }

    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    @Test
    public void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        UUID scriptId = UUID.randomUUID();
        remoteJsInvokeService.scriptIdToBodysMap.put(scriptId, "scriptscriptscript");

        String expectedInvocationResult = "scriptInvocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.doInvokeFunction(scriptId, "f", new Object[]{"a"}).get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptIdLSB()).isEqualTo(scriptId.getLeastSignificantBits());
        assertThat(jsInvokeRequestMade.getScriptIdMSB()).isEqualTo(scriptId.getMostSignificantBits());
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        UUID scriptId = UUID.randomUUID();
        String scriptBody = "scriptscriptscript";
        remoteJsInvokeService.scriptIdToBodysMap.put(scriptId, scriptBody);

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
        Object invocationResult = remoteJsInvokeService.doInvokeFunction(scriptId, "f", new Object[]{"a"}).get();
        verify(jsRequestTemplate, times(2)).send(jsRequestsCaptor.capture());

        List<TbProtoJsQueueMsg<RemoteJsRequest>> jsInvokeRequestsMade = jsRequestsCaptor.getAllValues();

        JsInvokeProtos.JsInvokeRequest firstRequestMade = jsInvokeRequestsMade.get(0).getValue().getInvokeRequest();
        assertThat(firstRequestMade.getScriptBody()).isNullOrEmpty();

        JsInvokeProtos.JsInvokeRequest secondRequestMade = jsInvokeRequestsMade.get(1).getValue().getInvokeRequest();
        assertThat(secondRequestMade.getScriptBody()).isEqualTo(scriptBody);

        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

}
