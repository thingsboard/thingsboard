// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.subscription.AddonType;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddonAccessRequestNotificationInfo implements NotificationInfo {

    private AddonType addonType;
    private String addonNameOverride;
    private String userEmail;
    private String enableAddonActionLabel;
    private String enableAddonLink;
    private String baseUrl;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "addon", StringUtils.isNotBlank(addonNameOverride) ? addonNameOverride : addonType.getAddonName(),
                "userEmail", userEmail,
                "enableAddonActionLabel", enableAddonActionLabel,
                "enableAddonLink", enableAddonLink,
                "baseUrl", baseUrl
        );
    }

}
