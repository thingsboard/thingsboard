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
package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public abstract class BaseEventFilter implements EventFilter {

    @ApiModelProperty(position = 1, value = "String value representing msg direction type (incoming to entity or outcoming from entity)", allowableValues = "IN, OUT")
    protected String msgDirectionType;
    @ApiModelProperty(position = 2, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @ApiModelProperty(position = 3, value = "The case insensitive 'contains' filter based on data (key and value) for the message.", example = "humidity")
    protected String dataSearch;
    @ApiModelProperty(position = 4, value = "The case insensitive 'contains' filter based on metadata (key and value) for the message.", example = "deviceName")
    protected String metadataSearch;
    @ApiModelProperty(position = 5, value = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityName;
    @ApiModelProperty(position = 6, value = "String value representing the type of message routing", example = "Success")
    protected String relationType;
    @ApiModelProperty(position = 7, value = "String value representing the entity id in the event body (originator of the message)", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String entityId;
    @ApiModelProperty(position = 8, value = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    @ApiModelProperty(position = 9, value = "Boolean value to filter the errors", allowableValues = "false, true")
    protected boolean isError;
    @ApiModelProperty(position = 10, value = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;
    @ApiModelProperty(position = 11, value = "String value representing the method name when the error happened", example = "onClusterEventMsg")
    protected String method;
    @ApiModelProperty(position = 12, value = "The minimum number of successfully processed messages", example = "25")
    protected Integer messagesProcessed;
    @ApiModelProperty(position = 13, value = "The minimum number of errors occurred during messages processing", example = "30")
    protected Integer errorsOccurred;
    @ApiModelProperty(position = 14, value = "String value representing the lifecycle event type", example = "STARTED")
    protected String event;
    @ApiModelProperty(position = 15, value = "String value representing status of the lifecycle event", allowableValues = "Success, Failure")
    protected String status;

}
