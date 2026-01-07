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
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilTest {

    final Exception cause = new RuntimeException();

    @Test
    void givenRootCause_whenLookupExceptionInCause_thenReturnRootCauseAndNoStackOverflow() {
        Exception e = cause;
        for (int i = 0; i <= 8000; i++) {
            e = new Exception(e);
        }
        assertThat(ExceptionUtil.lookupExceptionInCause(e, RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenCause_whenLookupExceptionInCause_thenReturnCause() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenNoCauseAndExceptionIsWantedCauseClass_whenLookupExceptionInCause_thenReturnSelf() {
        assertThat(ExceptionUtil.lookupExceptionInCause(cause, RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenNoCause_whenLookupExceptionInCause_thenReturnNull() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(), RuntimeException.class)).isNull();
    }

    @Test
    void givenNotWantedCause_whenLookupExceptionInCause_thenReturnNull() {
        final Exception cause = new IOException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isNull();
    }

    @Test
    void givenCause_whenLookupExceptionInCauseByMany_thenReturnFirstCause() {
        final Exception causeIAE = new IllegalAccessException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIAE);

        final Exception causeIOE = new IOException(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), ClassNotFoundException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIOE);
    }

}
