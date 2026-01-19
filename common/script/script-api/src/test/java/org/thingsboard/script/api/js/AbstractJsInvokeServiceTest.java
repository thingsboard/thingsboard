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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.thingsboard.server.common.stats.StatsCounter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class AbstractJsInvokeServiceTest {

    AbstractJsInvokeService service;
    final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = mock(AbstractJsInvokeService.class, Mockito.RETURNS_DEEP_STUBS);

        ReflectionTestUtils.setField(service, "requestsCounter", mock(StatsCounter.class));
        ReflectionTestUtils.setField(service, "evalCallback", mock(FutureCallback.class));

        // Make sure core checks always pass
        doReturn(true).when(service).isExecEnabled(any());
        doReturn(false).when(service).scriptBodySizeExceeded(anyString());
        doReturn(Futures.immediateFuture(id)).when(service).doEvalScript(any(), any(), anyString(), any(), any(String[].class));

        // Use real implementations
        doCallRealMethod().when(service).eval(any(), any(), any(), any(String[].class));
        doCallRealMethod().when(service).error(anyString());
        doCallRealMethod().when(service).validate(any(), anyString());
    }

    @Test
    void shouldReturnValidationErrorFromJsValidator() throws ExecutionException, InterruptedException {
        String scriptWithAsync = "async function test() {}";

        var future = service.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptWithAsync, "a", "b");
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertTrue(ex.getCause().getMessage().contains("Script must not contain 'async' keyword."));
        assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
        verify(service).isExecEnabled(any());
        verify(service).scriptBodySizeExceeded(any());
    }

    @Test
    void shouldPassValidationAndCallSuperEval() throws ExecutionException, InterruptedException, TimeoutException {
        String validScript = "function test() { return 42; }";
        var result = service.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, validScript, "x", "y");

        assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(id);
        verify(service, times(1)).isExecEnabled(any());
        verify(service, times(1)).scriptBodySizeExceeded(any());
    }

}
