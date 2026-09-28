// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import org.thingsboard.server.common.msg.TbMsg;

@Data
public class AggDeduplicationData {
    private final long ts;
    private final TbMsg msg;
}
