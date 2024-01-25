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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
@Builder
@RequiredArgsConstructor
public class AlarmRuleArgument implements Serializable {
    @Schema(description = "JSON object for specifying alarm condition by specific key")
    private final AlarmConditionFilterKey key;
    @Schema(description = "String representation of the type of the value", example = "NUMERIC")
    private final ArgumentValueType valueType;
    @Getter
    @NoXss
    private final Object defaultValue;

    private final ValueSourceType sourceType;

    private final boolean inherit;

    @JsonIgnore
    public boolean isDynamic() {
        return sourceType != null && !isConstant();
    }

    @JsonIgnore
    public boolean isConstant() {
        return key == null
                || key.getType() == null
                || StringUtils.isEmpty(key.getKey())
                || key.getType() == AlarmConditionKeyType.CONSTANT;
    }

    public enum ValueSourceType {
        CURRENT_TENANT,
        CURRENT_CUSTOMER,
        CURRENT_ENTITY
    }

}
