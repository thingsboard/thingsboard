// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
