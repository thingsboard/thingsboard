// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;

public record TraceGroupContribution(TraceGroup metrics, String sampleCandidateTraceId) {
}
