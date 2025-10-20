/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        @JsonSubTypes.Type(value = TbelCfTsRollingArg.class, name = "TS_ROLLING"),
        @JsonSubTypes.Type(value = TbelCfGeofencingArg.class, name = "GEOFENCING_CF_ARGUMENT_VALUE"),
        @JsonSubTypes.Type(value = TbelCfPropagationArg.class, name = "PROPAGATION_CF_ARGUMENT_VALUE"),
        @JsonSubTypes.Type(value = TbelCfLatestValuesAggregation.class, name = "LATEST_VALUES_AGGREGATION")
})
public interface TbelCfArg extends TbelCfObject {

    @JsonIgnore
    String getType();

}
