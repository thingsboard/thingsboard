// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface NotificationInfo {

    @JsonIgnore
    Map<String, String> getTemplateData();

    default EntityId getStateEntityId() {
        return null;
    }

    default DashboardId getDashboardId() {
        return null;
    }

    static NotificationInfo userActivated(String userFullName, String userEmail) {
        return UserActivatedNotificationInfo.builder()
                .userFullName(userFullName)
                .userEmail(userEmail)
                .build();
    }

    static NotificationInfo userRegistered(String userFullName, String userEmail) {
        return UserRegisteredNotificationInfo.builder()
                .userFullName(userFullName)
                .userEmail(userEmail)
                .build();
    }

}
