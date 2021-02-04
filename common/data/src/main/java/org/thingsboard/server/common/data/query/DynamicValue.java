/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

@Data
public class DynamicValue<T> {

    @JsonIgnore
    private T resolvedValue;

    @Getter
    private final DynamicValueSourceType sourceType;
    @Getter
    private final String sourceAttribute;

    @JsonCreator
    public DynamicValue(@JsonProperty("sourceType") DynamicValueSourceType sourceType,
                        @JsonProperty("sourceAttribute") String sourceAttribute) {
        this.sourceType = sourceType;
        this.sourceAttribute = sourceAttribute;
    }

}
