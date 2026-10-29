// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.id.ConverterId;

import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilTest {

    private final ConverterId converterId = new ConverterId(UUID.randomUUID());

    @Test
    void givenTbelScriptError_whenStackTraceDisabled_thenReturnsScriptErrorMessage() {
        var tbelError = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.RUNTIME, "",
                "Device name is missing.", new RuntimeException("Device name is missing."));

        String message = ExceptionUtil.toString(new ExecutionException(tbelError), converterId, false);

        assertThat(message).isEqualTo("Device name is missing.");
    }

    @Test
    void givenJsScriptError_whenStackTraceDisabled_thenReturnsScriptExceptionMessage() {
        var jsError = new TbScriptException(UUID.randomUUID(), TbScriptException.ErrorCode.RUNTIME, "",
                new ScriptException("Error: Device name is missing."));

        String message = ExceptionUtil.toString(new ExecutionException(jsError), converterId, false);

        assertThat(message).isEqualTo("Error: Device name is missing.");
    }

    @Test
    void givenUnknownError_whenStackTraceDisabled_thenHidesDetails() {
        String message = ExceptionUtil.toString(new IllegalStateException("internal"), converterId, false);

        assertThat(message).isEqualTo("Please contact system administrator");
    }

}
