// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

public interface RuleOriginatedNotificationInfo extends NotificationInfo {

    default CustomerId getAffectedCustomerId() {
        return null;
    }

    default UserId getAffectedUserId() {
        return null;
    }

    default TenantId getAffectedTenantId() {
        return null;
    }

}
