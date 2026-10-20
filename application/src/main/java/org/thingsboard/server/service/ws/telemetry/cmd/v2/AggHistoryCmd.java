// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Data;

import java.util.List;

@Data
public class AggHistoryCmd {

    private List<AggKey> keys;
    private long startTs;
    private long endTs;

}
