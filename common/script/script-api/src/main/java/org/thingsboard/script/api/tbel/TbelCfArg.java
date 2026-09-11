// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TbelCfSingleValueArg.class, name = "SINGLE_VALUE"),
        @JsonSubTypes.Type(value = TbelCfTsRollingArg.class, name = "TS_ROLLING")
})
public interface TbelCfArg extends TbelCfObject {

    @JsonIgnore
    String getType();

}
