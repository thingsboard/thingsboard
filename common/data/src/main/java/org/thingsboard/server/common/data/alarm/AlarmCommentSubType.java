// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import lombok.Getter;

public enum AlarmCommentSubType {

    ACKED_BY_USER("Alarm was acknowledged by user %s"),
    CLEARED_BY_USER("Alarm was cleared by user %s"),
    ASSIGNED_TO_USER("Alarm was assigned by user %s to user %s"),
    UNASSIGNED_BY_USER("Alarm was unassigned by user %s"),
    UNASSIGNED_FROM_DELETED_USER("Alarm was unassigned because user %s - was deleted"),
    COMMENT_DELETED("Comment was deleted by user %s"),
    SEVERITY_CHANGED("Alarm severity was updated from %s to %s");

    @Getter
    private final String text;

    AlarmCommentSubType(String text) {
        this.text = text;
    }
}
