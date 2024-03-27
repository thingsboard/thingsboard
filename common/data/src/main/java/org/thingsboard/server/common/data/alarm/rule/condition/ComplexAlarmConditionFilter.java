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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Schema
@Data
@AllArgsConstructor
public class ComplexAlarmConditionFilter implements AlarmConditionFilter, Serializable {

    @Serial
    private static final long serialVersionUID = 3216411668468598473L;

    private List<AlarmConditionFilter> conditions;

    private ComplexOperation operation;


    @Override
    public AlarmConditionType getType() {
        return AlarmConditionType.COMPLEX;
    }

    public enum ComplexOperation {
        AND,
        OR
    }
}
