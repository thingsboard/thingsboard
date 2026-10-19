// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Data;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggKey;

@Data
public class ReadTsKvQueryInfo {

    private final AggKey key;
    private final ReadTsKvQuery query;
    private final boolean previous;

}
