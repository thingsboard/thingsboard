/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@Builder
@AllArgsConstructor
public class AlarmComment extends BaseData<AlarmCommentId> implements HasName {
    @ApiModelProperty(position = 3, value = "JSON object with Alarm id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId alarmId;
    @ApiModelProperty(position = 4, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;
    @ApiModelProperty(position = 5, value = "Defines origination of comment. System type means comment was created by TB. OTHER type means comment was created by user.", example = "SYSTEM/OTHER", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private AlarmCommentType type;
    @ApiModelProperty(position = 6, value = "JSON object with text of comment.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @NoXss
    @Length(fieldName = "comment", max = 10000)
    private transient JsonNode comment;

    @ApiModelProperty(position = 1, value = "JSON object with the alarm comment Id. " +
            "Specify this field to update the alarm comment. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm." )
    @Override
    public AlarmCommentId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the alarm comment creation, in milliseconds", example = "1634058704567", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public AlarmComment() {
        super();
    }

    public AlarmComment(AlarmCommentId id) {
        super(id);
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 5, required = true, value = "representing comment text", example = "Please take a look")
    public String getName() {
        return comment.toString();
    }

    public AlarmComment(AlarmComment alarmComment) {
        super(alarmComment.getId());
        this.createdTime = alarmComment.getCreatedTime();
        this.alarmId = alarmComment.getAlarmId();
        this.type = alarmComment.getType();
        this.comment = alarmComment.getComment();
        this.userId = alarmComment.getUserId();
    }
}
