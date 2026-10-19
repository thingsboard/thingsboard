// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import lombok.Data;

@Data
public class AffectedUserFilter implements UsersFilter {

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.AFFECTED_USER;
    }

}
