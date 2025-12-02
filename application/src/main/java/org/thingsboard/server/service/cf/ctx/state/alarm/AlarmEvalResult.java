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
