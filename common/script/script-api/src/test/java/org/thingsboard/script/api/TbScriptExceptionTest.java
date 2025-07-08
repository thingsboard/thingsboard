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
