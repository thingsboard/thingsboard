// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TbelCfSingleValueArg implements TbelCfArg {

    private final long ts;
    private final Object value;

    @JsonCreator
    public TbelCfSingleValueArg(
            @JsonProperty("ts") long ts,
            @JsonProperty("value") Object value
    ) {
        this.ts = ts;
        this.value = value;
    }

    @Override
    public long memorySize() {
        if (value instanceof String strValue) {
            return OBJ_SIZE + strValue.length();
        } else {
            return OBJ_SIZE;
        }
    }

    @Override
    public String getType() {
        return "SINGLE_VALUE";
    }

}
