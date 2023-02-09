/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;

import java.io.Serializable;

@ApiModel
@Data
public class AlarmConditionFilter implements Serializable {

    private static final long serialVersionUID = 186010644723236779L;

    @Valid
    @ApiModelProperty(position = 1, value = "JSON object for specifying alarm condition by specific key")
    private AlarmConditionFilterKey key;
    @ApiModelProperty(position = 2, value = "String representation of the type of the value", example = "NUMERIC")
    private EntityKeyValueType valueType;
    @NoXss
    @ApiModelProperty(position = 3, value = "Value used in Constant comparison. For other types, such as TIME_SERIES or ATTRIBUTE, the predicate condition is used")
    private Object value;
    @Valid
    @ApiModelProperty(position = 4, value = "JSON object representing filter condition")
    private KeyFilterPredicate predicate;

}
