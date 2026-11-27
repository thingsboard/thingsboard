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
public class AiCreditsLimitIncreaseRequestNotificationInfo implements NotificationInfo {

    private String userEmail;
    private String tenantName;
    private String limitDescription;
    private String increaseLimitActionLabel;
    private String increaseLimitLink;
    private String baseUrl;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "userEmail", userEmail,
                "tenantName", tenantName,
                "limitDescription", limitDescription,
                "increaseLimitActionLabel", increaseLimitActionLabel,
                "increaseLimitLink", increaseLimitLink,
                "baseUrl", baseUrl
        );
    }

}
