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
package org.thingsboard.server.common.data.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.UserId;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Notification extends SearchTextBased<NotificationId> {

    private NotificationRequestId requestId;
    private UserId recipientId;
    private String reason;
    private String text;
    private NotificationInfo info;
    private NotificationSeverity severity;
    private NotificationStatus status;
//    private UserId senderId;

    @Override
    public String getSearchText() {
        return text;
    }
}
