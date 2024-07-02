/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm.rule.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;

@Schema
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConstantArgument extends AbstractArgument {

    @Serial
    private static final long serialVersionUID = 6364879917733086328L;

    private final Object value;
    private final String description;

    public ConstantArgument(ArgumentValueType valueType, Object value) {
        this(valueType, value, null);
    }

    @JsonCreator
    public ConstantArgument(@JsonProperty("valueType") ArgumentValueType valueType,
                            @JsonProperty("value") Object value,
                            @JsonProperty("description") String description) {
        super(valueType);
        this.value = value;
        this.description = description;
    }

    @Override
    public ArgumentType getType() {
        return ArgumentType.CONSTANT;
    }

    @Override
    @JsonIgnore
    public AlarmConditionFilterKey getKey() {
        return null;
    }
}
