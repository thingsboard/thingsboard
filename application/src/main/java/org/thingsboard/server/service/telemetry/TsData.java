// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class TsData implements Comparable<TsData>{

    private final long ts;
    private final Object value;

    public TsData(long ts, Object value) {
        super();
        this.ts = ts;
        this.value = value;
    }

    @Schema(description = "Timestamp last updated timeseries, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    public long getTs() {
        return ts;
    }

    @Schema(description = "Object representing value of timeseries key", example = "20", accessMode = Schema.AccessMode.READ_ONLY)
    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(TsData o) {
        return Long.compare(ts, o.ts);
    }

}
