// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.sub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.NotificationRequestId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestUpdate {
    private NotificationRequestId notificationRequestId;
    private boolean deleted;
}
