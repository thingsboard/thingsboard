// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanEventEntity {
    private UUID id;

    private String spanId;

    private String traceId;

    private String eventName;

    private long eventTime;

    private String attributesJson;

    private long createdTime;
}
