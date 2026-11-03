// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourcesShortageNotificationInfo implements RuleOriginatedNotificationInfo {

    private String resource;
    private Long usage;
    private String serviceId;
    private String serviceType;

    @Override
    public Map<String, String> getTemplateData() {
        return Map.of(
                "resource", resource,
                "usage", String.valueOf(usage),
                "serviceId", serviceId,
                "serviceType", serviceType
        );
    }

}
