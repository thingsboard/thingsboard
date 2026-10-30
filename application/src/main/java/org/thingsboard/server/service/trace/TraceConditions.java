// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

public final class TraceConditions {
    public static final String TRACE_ENABLED =
            "('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-rule-engine')"
                    + " && '${otel.tracing.enabled:null}'=='true'";

    public static final String SHARED_CACHE = TRACE_ENABLED + " && '${cache.type:caffeine}'=='redis'";

    public static final String LOCAL_CACHE = TRACE_ENABLED + " && '${cache.type:caffeine}'!='redis'";

    private TraceConditions() {
    }
}
