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
package org.thingsboard.server.common.data.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmCommentInfo extends AlarmComment {
    private static final long serialVersionUID = 2807343093519543377L;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Brown")
    private String lastName;

    @Schema(description = "User email address", example = "johnBrown@gmail.com")
    private String email;

    public AlarmCommentInfo() {
        super();
    }

    public AlarmCommentInfo(AlarmComment alarmComment) {
        super(alarmComment);
    }

    public AlarmCommentInfo(AlarmComment alarmComment, String firstName, String lastName, String email) {
        super(alarmComment);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
