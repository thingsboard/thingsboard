// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TraceRetryMetadata {
    String tenantId;
    String traceId;
    String traceparent;
    String spanOnRetry;
    String traceRecordMsgPayload;
}
