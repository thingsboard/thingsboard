// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.targets.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum UsersFilterType {

    USER_LIST,
    USER_GROUP_LIST,
    CUSTOMER_USERS,
    TENANT_ADMINISTRATORS,
    AFFECTED_TENANT_ADMINISTRATORS(true),
    SYSTEM_ADMINISTRATORS,
    USER_ROLE,
    ALL_USERS,
    ORIGINATOR_ENTITY_OWNER_USERS(true),
    AFFECTED_USER(true);

    private boolean forRules;

}
