// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerUsersFilter implements UsersFilter {

    @NotNull
    private UUID customerId;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.CUSTOMER_USERS;
    }

}
