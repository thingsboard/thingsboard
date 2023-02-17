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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;

@Data
public class CreateOrUpdateActiveAlarmRequest {

    @ApiModelProperty(position = 1, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @ApiModelProperty(position = 2, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @ApiModelProperty(position = 3, required = true, value = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    @ApiModelProperty(position = 4, required = true, value = "JSON object with alarm originator id")
    private EntityId originator;
    @ApiModelProperty(position = 5, required = true, value = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @ApiModelProperty(position = 6, value = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @ApiModelProperty(position = 7, value = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    @ApiModelProperty(position = 8, value = "JSON object with alarm details")
    private JsonNode details;
    @ApiModelProperty(position = 9, value = "JSON object with propagation details")
    private AlarmPropagationInfo propagation;

}
