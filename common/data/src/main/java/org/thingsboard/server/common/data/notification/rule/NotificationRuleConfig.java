// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule;

import lombok.Data;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Data
public class NotificationRuleConfig implements Serializable {

    @NoXss
    private String description;

}
