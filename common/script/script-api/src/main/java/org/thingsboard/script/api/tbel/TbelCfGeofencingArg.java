// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TbelCfGeofencingArg implements TbelCfArg {

    private final Object value;

    @JsonCreator
    public TbelCfGeofencingArg(@JsonProperty("value") Object value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return "GEOFENCING_CF_ARGUMENT_VALUE";
    }


    @Override
    public long memorySize() {
        return OBJ_SIZE;
    }

}
