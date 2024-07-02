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
public class AttributeArgument extends AbstractArgument {

    @Serial
    private static final long serialVersionUID = -2213808682577271974L;

    private final String attribute;

    private final Object defaultValue;

    private final ValueSourceType sourceType;

    private final boolean inherit;

    @JsonCreator
    public AttributeArgument(@JsonProperty("attribute") String attribute,
                             @JsonProperty("valueType") ArgumentValueType valueType,
                             @JsonProperty("sourceType") ValueSourceType sourceType,
                             @JsonProperty("defaultValue") Object defaultValue,
                             @JsonProperty("inherit") boolean inherit) {
        super(valueType);
        this.attribute = attribute;
        this.defaultValue = defaultValue;
        this.sourceType = sourceType;
        this.inherit = inherit;
    }

    @Override
    public ArgumentType getType() {
        return ArgumentType.ATTRIBUTE;
    }

    @Override
    @JsonIgnore
    public AlarmConditionFilterKey getKey() {
        return new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, attribute);
    }
}
