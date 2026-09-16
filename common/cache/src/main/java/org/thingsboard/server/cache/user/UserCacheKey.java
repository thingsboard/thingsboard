// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.user;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@RequiredArgsConstructor
public class UserCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 7357353074893750678L;

    @NonNull
    private final TenantId tenantId;
    private final String email;

    @Override
    public String toString() {
        return tenantId.getId() + "_" + email;
    }

}
