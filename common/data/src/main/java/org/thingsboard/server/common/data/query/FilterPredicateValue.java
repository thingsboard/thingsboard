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
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;

import java.io.Serializable;

@Data
public class FilterPredicateValue<T> implements Serializable {

    @Getter
    @NoXss
    private final T defaultValue;
    @Getter
    @NoXss
    private final T userValue;
    @Getter
    @Valid
    private final DynamicValue<T> dynamicValue;

    public FilterPredicateValue(T defaultValue) {
        this(defaultValue, null, null);
    }

    @JsonCreator
    public FilterPredicateValue(@JsonProperty("defaultValue") T defaultValue,
                                @JsonProperty("userValue") T userValue,
                                @JsonProperty("dynamicValue") DynamicValue<T> dynamicValue) {
        this.defaultValue = defaultValue;
        this.userValue = userValue;
        this.dynamicValue = dynamicValue;
    }

    @JsonIgnore
    public T getValue() {
        if (this.userValue != null) {
            return this.userValue;
        } else {
            if (this.dynamicValue != null && this.dynamicValue.getResolvedValue() != null) {
                return this.dynamicValue.getResolvedValue();
            } else {
                return defaultValue;
            }
        }
    }

    public static FilterPredicateValue<Double> fromDouble(double value) {
        return new FilterPredicateValue<>(value);
    }

    public static FilterPredicateValue<String> fromString(String value) {
        return new FilterPredicateValue<>(value);
    }

    public static FilterPredicateValue<Boolean> fromBoolean(boolean value) {
        return new FilterPredicateValue<>(value);
    }
}
