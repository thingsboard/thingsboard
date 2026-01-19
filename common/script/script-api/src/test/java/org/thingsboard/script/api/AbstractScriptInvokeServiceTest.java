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
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsCounter;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AbstractScriptInvokeServiceTest {

    AbstractScriptInvokeService service;
    final UUID id = UUID.randomUUID();
    final String scriptBody = "return true;";
    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("2ed9a658-45a5-4812-b212-9931f5749f30"));

    @BeforeEach
    void setUp() {
        service = mock(AbstractScriptInvokeService.class, Mockito.RETURNS_DEEP_STUBS);

        // Make sure core checks always pass
        doReturn(true).when(service).isExecEnabled(any());
        doReturn(50000L).when(service).getMaxScriptBodySize();

        // Use real implementations
        doCallRealMethod().when(service).scriptBodySizeExceeded(anyString());
        doCallRealMethod().when(service).eval(any(), any(), any(), any(String[].class));
        doCallRealMethod().when(service).error(anyString());
        doCallRealMethod().when(service).validate(any(), anyString());
    }

    @Test
    void evalWithValidationCallTest() throws ExecutionException, InterruptedException, TimeoutException {
        ReflectionTestUtils.setField(service, "requestsCounter", mock(StatsCounter.class));
        ReflectionTestUtils.setField(service, "evalCallback", mock(FutureCallback.class));

        doReturn(Futures.immediateFuture(id)).when(service).doEvalScript(any(), any(), anyString(), any(), any(String[].class));

        var future = service.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, scriptBody, "x", "y");

        assertThat(future.get(30, TimeUnit.SECONDS)).isEqualTo(id);
        verify(service).validate(any(), anyString());
        verify(service).validate(tenantId, scriptBody);
        verify(service, never()).error(anyString());
    }

    @Test
    void evalWithValidationCallErrorTest() throws ExecutionException, InterruptedException, TimeoutException {
        doReturn(false).when(service).isExecEnabled(any());
        var future = service.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, scriptBody, "x", "y");

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertThat(ex.getCause().getMessage()).isEqualTo("Script Execution is disabled due to API limits!");
        assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);

        verify(service).validate(any(), anyString());
        verify(service).validate(tenantId, scriptBody);
        verify(service).error(anyString());
    }

    @Test
    void validateScriptBodyTestExecEnabledTest() {
        assertNull(service.validate(tenantId, scriptBody));
        verify(service).isExecEnabled(tenantId);
    }

    @Test
    void validateScriptBodyTestExecDisabledTest() {
        doReturn(false).when(service).isExecEnabled(tenantId);
        assertThat(service.validate(tenantId, scriptBody)).isEqualTo("Script Execution is disabled due to API limits!");
        verify(service).isExecEnabled(tenantId);
    }

    @Test
    void validateScriptBodySizeOKTest() {
        assertNull(service.validate(tenantId, scriptBody));
        verify(service).isExecEnabled(tenantId);
        verify(service).scriptBodySizeExceeded(scriptBody);
    }

    @Test
    void validateScriptBodySizeExceededTest() {
        doReturn(10L).when(service).getMaxScriptBodySize();
        assertThat(service.validate(tenantId, scriptBody)).isEqualTo("Script body exceeds maximum allowed size of 10 symbols");
        verify(service).isExecEnabled(tenantId);
        verify(service).scriptBodySizeExceeded(scriptBody);
    }

}
