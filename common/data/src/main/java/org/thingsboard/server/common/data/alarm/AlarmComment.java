/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@Builder
@AllArgsConstructor
public class AlarmComment extends BaseData<AlarmCommentId> implements HasName {

    @Serial
    private static final long serialVersionUID = -5454905526404017592L;

    @Schema(description = "JSON object with Alarm id.", accessMode = Schema.AccessMode.READ_ONLY)
    private AlarmId alarmId;
    @Schema(description = "JSON object with User id.", accessMode = Schema.AccessMode.READ_ONLY)
    private UserId userId;
    @Schema(description = "Defines origination of comment. System type means comment was created by TB. OTHER type means comment was created by user.", example = "SYSTEM/OTHER", accessMode = Schema.AccessMode.READ_ONLY)
    private AlarmCommentType type;
    @Schema(description = "JSON object with text of comment.")
    @NoXss
    @Length(fieldName = "comment", max = 10000)
    @EqualsAndHashCode.Include
    private JsonNode comment;

    @Schema(description = "JSON object with the alarm comment Id. " +
            "Specify this field to update the alarm comment. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public AlarmCommentId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the alarm comment creation, in milliseconds", example = "1634058704567", accessMode = Schema.AccessMode.READ_ONLY)
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
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "representing comment text", example = "Please take a look")
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
