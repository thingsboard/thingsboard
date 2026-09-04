// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
