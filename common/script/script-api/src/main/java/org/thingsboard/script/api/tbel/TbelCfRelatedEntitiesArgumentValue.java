// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Data
public class TbelCfRelatedEntitiesArgumentValue implements TbelCfArg {

    private final Map<UUID, TbelCfSingleValueArg> entityInputs;

    @JsonCreator
    public TbelCfRelatedEntitiesArgumentValue(@JsonProperty("entityInputs") Map<UUID, TbelCfSingleValueArg> values) {
        this.entityInputs = Collections.unmodifiableMap(values);
    }

    @Override
    public String getType() {
        return "RELATED_ENTITIES_ARGUMENT_VALUE";
    }

    @Override
    public long memorySize() {
        return OBJ_SIZE;
    }
}
