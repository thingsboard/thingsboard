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

import lombok.Getter;

public enum AlarmCommentSubType {

    ACKED_BY_USER("Alarm was acknowledged by user %s"),
    CLEARED_BY_USER("Alarm was cleared by user %s"),
    ASSIGNED_TO_USER("Alarm was assigned by user %s to user %s"),
    UNASSIGNED_BY_USER("Alarm was unassigned by user %s"),
    UNASSIGNED_FROM_DELETED_USER("Alarm was unassigned because user %s - was deleted"),
    COMMENT_DELETED("User %s deleted his comment"),
    SEVERITY_CHANGED("Alarm severity was changed by user %s from %s to %s");

    @Getter
    private final String text;

    AlarmCommentSubType(String text) {
        this.text = text;
    }
}
