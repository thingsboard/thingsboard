/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
public class AlarmConditionFilter implements Serializable {

    @Valid
    @Schema(description = "JSON object for specifying alarm condition by specific key")
    private AlarmConditionFilterKey key;
    @Schema(description = "String representation of the type of the value", example = "NUMERIC")
    private EntityKeyValueType valueType;
    @NoXss
    @Schema(description = "Value used in Constant comparison. For other types, such as TIME_SERIES or ATTRIBUTE, the predicate condition is used")
    private Object value;
    @Valid
    @Schema(description = "JSON object representing filter condition")
    private KeyFilterPredicate predicate;

}
