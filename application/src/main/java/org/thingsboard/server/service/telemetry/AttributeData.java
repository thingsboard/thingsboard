// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.telemetry;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class AttributeData implements Comparable<AttributeData>{

    private final long lastUpdateTs;
    private final String key;
    private final Object value;

    public AttributeData(long lastUpdateTs, String key, Object value) {
        super();
        this.lastUpdateTs = lastUpdateTs;
        this.key = key;
        this.value = value;
    }

    @Schema(description = "Timestamp last updated attribute, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    public long getLastUpdateTs() {
        return lastUpdateTs;
    }

    @Schema(description = "String representing attribute key", example = "active", accessMode = Schema.AccessMode.READ_ONLY)
    public String getKey() {
        return key;
    }

    @Schema(description = "Object representing value of attribute key", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(AttributeData o) {
        return Long.compare(lastUpdateTs, o.lastUpdateTs);
    }

}
