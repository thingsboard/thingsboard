// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Data;
import org.thingsboard.server.common.data.kv.Aggregation;

@Data
public class AggKey {

    private int id;
    private String key;
    private Aggregation agg;

    private Long previousStartTs;
    private Long previousEndTs;
    private Boolean previousValueOnly;

}
