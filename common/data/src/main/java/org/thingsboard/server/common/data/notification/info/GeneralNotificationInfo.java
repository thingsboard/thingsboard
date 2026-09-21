// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralNotificationInfo implements RuleOriginatedNotificationInfo {

    private Map<String, String> data;

    @Override
    public Map<String, String> getTemplateData() {
        return data;
    }

}
