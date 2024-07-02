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
import lombok.Data;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.io.Serializable;

@Schema
@Data
public class SimpleAlarmConditionFilter implements AlarmConditionFilter, Serializable {

    @Serial
    private static final long serialVersionUID = 4931296874864969049L;

    @NoXss
    private String leftArgId;
    @NoXss
    private String rightArgId;

    private ArgumentOperation operation;

    //Only for String
    private boolean ignoreCase;

    @Override
    public AlarmConditionType getType() {
        return AlarmConditionType.SIMPLE;
    }
}
