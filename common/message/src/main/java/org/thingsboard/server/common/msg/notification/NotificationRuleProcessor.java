// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.notification;

import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;

public interface NotificationRuleProcessor {

    void process(NotificationRuleTrigger trigger);

}
