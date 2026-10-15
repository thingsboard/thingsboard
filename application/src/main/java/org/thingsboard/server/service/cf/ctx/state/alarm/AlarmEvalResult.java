// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.alarm;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AlarmEvalResult {

    public static final AlarmEvalResult TRUE = new AlarmEvalResult(Status.TRUE);
    public static final AlarmEvalResult FALSE = new AlarmEvalResult(Status.FALSE);
    public static final AlarmEvalResult NOT_YET_TRUE = new AlarmEvalResult(Status.NOT_YET_TRUE);
    public static final AlarmEvalResult EMPTY = new AlarmEvalResult(null);

    private final Status status;
    private final long leftDuration;
    private final long leftEvents;
    private Cause cause;

    public AlarmEvalResult(Status status) {
        this(status, 0, 0);
    }

    public static AlarmEvalResult notYetTrue(long leftEvents, long leftDuration) {
        return new AlarmEvalResult(Status.NOT_YET_TRUE, leftDuration, leftEvents);
    }

    public AlarmEvalResult withCause(Cause cause) {
        this.cause = cause;
        return this;
    }

    public enum Status {
        FALSE, NOT_YET_TRUE, TRUE;
    }

    public enum Cause {
        NEW_EVENT, SCHEDULED_REEVALUATION;
    }

}
