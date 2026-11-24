// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import org.junit.jupiter.api.Test;
import org.mvel2.CompileException;
import org.mvel2.ScriptRuntimeException;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.exception.ThingsboardRuntimeException;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilTest {

    private static final IntegrationId INTEGRATION_ID = new IntegrationId(UUID.randomUUID());
    private static final String MASKED = "Please contact system administrator";

    @Test
    void showsThingsboardRuntimeExceptionMessage() {
        var e = new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo("Creating devices is forbidden!");
    }

    @Test
    void showsThingsboardExceptionMessage() {
        var e = new ThingsboardException("ChirpStack rejected downlink (401 UNAUTHORIZED): ", ThingsboardErrorCode.BAD_REQUEST_PARAMS);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo("ChirpStack rejected downlink (401 UNAUTHORIZED): ");
    }

    @Test
    void showsThingsboardExceptionMessageFromCause() {
        var cause = new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
        var e = new ExecutionException(new RuntimeException("internal details", cause));

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo("Creating devices is forbidden!");
    }

    @Test
    void masksThingsboardExceptionWithoutMessage() {
        var e = new ThingsboardException(ThingsboardErrorCode.GENERAL);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo(MASKED);
    }

    @Test
    void showsTbelRuntimeErrorMessage() {
        var cause = new ScriptRuntimeException("Cannot invoke method foo on null");
        var e = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.RUNTIME, "return msg.foo();", cause);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo("Cannot invoke method foo on null");
    }

    @Test
    void showsTbelCompilationErrorMessage() {
        var cause = new CompileException("unexpected token", "return {".toCharArray(), 7);
        var e = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.COMPILATION, "return {", cause);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo(cause.getMessage());
    }

    @Test
    void showsRemoteJsRuntimeErrorMessageFromCause() {
        var scriptException = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.RUNTIME, null,
                new RuntimeException("ReferenceError: x is not defined"));
        var e = new ExecutionException(scriptException);

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo("ReferenceError: x is not defined");
    }

    @Test
    void masksScriptErrorsOtherThanRuntimeAndCompilation() {
        var other = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.OTHER, null,
                new RuntimeException("SELECT * FROM device"));
        var timeout = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.TIMEOUT, null,
                new RuntimeException("SELECT * FROM device"));

        assertThat(ExceptionUtil.toString(other, INTEGRATION_ID, false)).isEqualTo(MASKED);
        assertThat(ExceptionUtil.toString(timeout, INTEGRATION_ID, false)).isEqualTo(MASKED);
    }

    @Test
    void masksOtherExceptions() {
        var e = new RuntimeException("SELECT * FROM device");

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo(MASKED);
    }

    @Test
    void thingsboardErrorIsShownInDebugEvent() {
        var cause = new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);

        assertThat(ExceptionUtil.isShownInDebugEvent(new ExecutionException(cause))).isTrue();
    }

    @Test
    void scriptRuntimeErrorIsShownInDebugEvent() {
        var e = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.RUNTIME, null, new RuntimeException("Index 0 out of bounds"));

        assertThat(ExceptionUtil.isShownInDebugEvent(new ExecutionException(e))).isTrue();
    }

    @Test
    void maskedErrorIsNotShownInDebugEvent() {
        assertThat(ExceptionUtil.isShownInDebugEvent(new RuntimeException("SELECT * FROM device"))).isFalse();
        assertThat(ExceptionUtil.isShownInDebugEvent(new ThingsboardException(ThingsboardErrorCode.GENERAL))).isFalse();
        assertThat(ExceptionUtil.isShownInDebugEvent(new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.TIMEOUT, null,
                new RuntimeException("Script timeout!")))).isFalse();
    }

}
