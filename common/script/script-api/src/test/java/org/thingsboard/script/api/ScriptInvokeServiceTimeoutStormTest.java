// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.stats.StatsType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptInvokeServiceTimeoutStormTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("2ed9a658-45a5-4812-b212-9931f5749f30"));
    TestScriptInvokeService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    void singleScriptIsBlockedAfterMaxErrorsTimeouts() throws Exception {
        service = newService(true);
        UUID a = service.addScript(Outcome.TIMEOUT);

        timeout(a, 3);

        assertBlocked(a);
    }

    @Test
    void timeoutStormDoesNotBlockScripts() throws Exception {
        service = newService(true);
        UUID a = service.addScript(Outcome.TIMEOUT);
        UUID b = service.addScript(Outcome.TIMEOUT);
        UUID c = service.addScript(Outcome.TIMEOUT);

        timeout(a, 1);
        timeout(b, 1);
        timeout(c, 1); // storm starts
        timeout(a, 5);
        timeout(b, 5);
        timeout(c, 5);

        assertThat(service.disabledScripts).isEmpty();
        assertNotBlocked(a);
    }

    @Test
    void stormUnblocksScriptsBlockedOnlyByStormTimeouts() throws Exception {
        service = newService(true);
        UUID a = service.addScript(Outcome.TIMEOUT);
        UUID b = service.addScript(Outcome.TIMEOUT);
        UUID c = service.addScript(Outcome.TIMEOUT);

        timeout(a, 3);
        assertBlocked(a);

        timeout(b, 1);
        timeout(c, 1); // storm starts: 'a' was blocked by timeouts only, within the window

        assertNotBlocked(a);
    }

    @Test
    void stormKeepsScriptsBlockedByOtherErrors() throws Exception {
        service = newService(true);
        UUID a = service.addScript(Outcome.COMPILATION_ERROR);
        UUID b = service.addScript(Outcome.TIMEOUT);
        UUID c = service.addScript(Outcome.TIMEOUT);
        UUID d = service.addScript(Outcome.TIMEOUT);

        fail(a, 3);
        assertBlocked(a);

        timeout(b, 1);
        timeout(c, 1);
        timeout(d, 1);

        assertBlocked(a);
    }

    @Test
    void nonTimeoutErrorsAreCountedDuringStorm() throws Exception {
        service = newService(true);
        UUID b = service.addScript(Outcome.TIMEOUT);
        UUID c = service.addScript(Outcome.TIMEOUT);
        UUID d = service.addScript(Outcome.TIMEOUT);
        UUID a = service.addScript(Outcome.COMPILATION_ERROR);

        timeout(b, 1);
        timeout(c, 1);
        timeout(d, 1);
        fail(a, 3);

        assertBlocked(a);
    }

    @Test
    void guardDisabledKeepsPreviousBehavior() throws Exception {
        service = newService(false);
        UUID a = service.addScript(Outcome.TIMEOUT);
        UUID b = service.addScript(Outcome.TIMEOUT);
        UUID c = service.addScript(Outcome.TIMEOUT);

        timeout(a, 3);
        timeout(b, 3);
        timeout(c, 3);

        assertBlocked(a);
        assertBlocked(b);
        assertBlocked(c);
    }

    private TestScriptInvokeService newService(boolean timeoutStormEnabled) {
        TestScriptInvokeService s = new TestScriptInvokeService(timeoutStormEnabled);
        ReflectionTestUtils.setField(s, "statsFactory", Optional.empty());
        s.init();
        return s;
    }

    private void timeout(UUID scriptId, int times) {
        for (int i = 0; i < times; i++) {
            assertThatThrownBy(() -> invoke(scriptId)).hasCauseInstanceOf(TimeoutException.class);
        }
    }

    private void fail(UUID scriptId, int times) {
        for (int i = 0; i < times; i++) {
            assertThatThrownBy(() -> invoke(scriptId)).hasCauseInstanceOf(TbScriptException.class);
        }
    }

    private void assertBlocked(UUID scriptId) {
        assertThatThrownBy(() -> invoke(scriptId))
                .hasRootCauseMessage("Script invocation is blocked due to maximum error count 3, scriptId " + scriptId + "!");
    }

    private void assertNotBlocked(UUID scriptId) {
        assertThatThrownBy(() -> invoke(scriptId))
                .rootCause().message().doesNotStartWith("Script invocation is blocked");
    }

    private Object invoke(UUID scriptId) throws ExecutionException, InterruptedException {
        return service.invokeScript(tenantId, null, scriptId).get();
    }

    enum Outcome {TIMEOUT, COMPILATION_ERROR}

    static class TestScriptInvokeService extends AbstractScriptInvokeService {

        private final boolean timeoutStormEnabled;
        private final Map<UUID, Outcome> scripts = new ConcurrentHashMap<>();

        TestScriptInvokeService(boolean timeoutStormEnabled) {
            this.timeoutStormEnabled = timeoutStormEnabled;
        }

        UUID addScript(Outcome outcome) {
            UUID id = UUID.randomUUID();
            scripts.put(id, outcome);
            return id;
        }

        @Override
        protected boolean isTimeoutStormEnabled() {
            return timeoutStormEnabled;
        }

        @Override
        protected TbScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
            Throwable error = scripts.get(scriptId) == Outcome.TIMEOUT
                    ? new TimeoutException("Request timeout")
                    : new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, null, new RuntimeException("compile"));
            return new TbScriptExecutionTask(Futures.immediateFailedFuture(error)) {
                @Override
                public void stop() {
                }
            };
        }

        @Override
        protected long getMaxInvokeRequestsTimeout() {
            return 0;
        }

        @Override
        protected long getMaxScriptBodySize() {
            return 0;
        }

        @Override
        protected long getMaxTotalArgsSize() {
            return 0;
        }

        @Override
        protected long getMaxResultSize() {
            return 0;
        }

        @Override
        protected int getMaxBlackListDurationSec() {
            return 60;
        }

        @Override
        protected int getMaxErrors() {
            return 3;
        }

        @Override
        protected boolean isStatsEnabled() {
            return false;
        }

        @Override
        protected String getStatsName() {
            return "Test Invoke Stats";
        }

        @Override
        protected Executor getCallbackExecutor() {
            return MoreExecutors.directExecutor();
        }

        @Override
        protected boolean isScriptPresent(UUID scriptId) {
            return scripts.containsKey(scriptId);
        }

        @Override
        protected boolean isExecEnabled(TenantId tenantId) {
            return true;
        }

        @Override
        protected void reportExecution(TenantId tenantId, CustomerId customerId) {
        }

        @Override
        protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
            return Futures.immediateFuture(scriptId);
        }

        @Override
        protected void doRelease(UUID scriptId) {
        }

        @Override
        protected StatsType getStatsType() {
            return StatsType.JS_INVOKE;
        }

        @Override
        public ScriptLanguage getLanguage() {
            return ScriptLanguage.JS;
        }
    }

}
