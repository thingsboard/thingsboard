// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum UsersFilterType {

    USER_LIST,
    CUSTOMER_USERS,
    TENANT_ADMINISTRATORS,
    AFFECTED_TENANT_ADMINISTRATORS(true),
    SYSTEM_ADMINISTRATORS,
    ALL_USERS,
    ORIGINATOR_ENTITY_OWNER_USERS(true),
    AFFECTED_USER(true);

    private boolean forRules;

}
