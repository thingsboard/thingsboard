// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntitiesLimitIncreaseRequestNotificationInfo implements NotificationInfo {

    private EntityType entityType;
    private String userEmail;
    private String increaseLimitActionLabel;
    private String increaseLimitLink;
    private String baseUrl;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "entityType", entityType.getNormalName(),
                "userEmail", userEmail,
                "increaseLimitActionLabel", increaseLimitActionLabel,
                "increaseLimitLink", increaseLimitLink,
                "baseUrl", baseUrl
        );
    }
}
