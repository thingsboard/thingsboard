// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import org.junit.jupiter.api.Test;
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
    void masksOtherExceptions() {
        var e = new RuntimeException("SELECT * FROM device");

        assertThat(ExceptionUtil.toString(e, INTEGRATION_ID, false)).isEqualTo(MASKED);
    }

}
