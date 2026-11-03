// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TbScriptExceptionTest {

    @Test
    void givenCompilationError_whenCheckingIsUnrecoverable_thenReturnsTrue() {
        // GIVEN
        var exception = new TbScriptException(null, TbScriptException.ErrorCode.COMPILATION, null, null);

        // WHEN-THEN
        assertThat(exception.isUnrecoverable()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = TbScriptException.ErrorCode.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = "COMPILATION"
    )
    void givenRecoverableErrorCodes_whenCheckingIsUnrecoverable_thenReturnsFalse(TbScriptException.ErrorCode errorCode) {
        // GIVEN
        var exception = new TbScriptException(null, errorCode, null, null);

        // WHEN-THEN
        assertThat(exception.isUnrecoverable()).isFalse();
    }

}
