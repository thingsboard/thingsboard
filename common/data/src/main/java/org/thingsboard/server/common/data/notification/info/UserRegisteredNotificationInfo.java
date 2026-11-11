// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredNotificationInfo implements NotificationInfo {

    private String userFullName;
    private String userEmail;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "userFullName", userFullName,
                "userEmail", userEmail
        );
    }

}
