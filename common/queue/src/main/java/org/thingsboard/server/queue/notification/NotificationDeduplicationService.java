// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.notification;

import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;

public interface NotificationDeduplicationService {

    boolean alreadyProcessed(NotificationRuleTrigger trigger);

    boolean alreadyProcessed(NotificationRuleTrigger trigger, NotificationRule rule);

}
